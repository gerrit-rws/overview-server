package controllers

import java.sql.Connection
import java.util.UUID
import models.orm.SquerylPostgreSqlAdapter
import models.upload.OverviewUpload
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Session
import play.api.Play.current
import play.api.db.DB
import play.api.libs.iteratee.Error
import play.api.libs.iteratee.Input
import play.api.libs.iteratee.Iteratee
import play.api.mvc.{ Action, BodyParser, BodyParsers, Request, RequestHeader, Result }
import scalax.io.Input

/**
 * Handles a file upload, storing the file in a LargeObject, updating the upload table,
 * and starting a DocumentSetCreationJob. Most of the work related to the upload happens
 * in FileUploadIteratee.
 */
trait UploadController extends BaseController {

  // authorizeInTransaction and authorizedBodyParser don't belong here.
  // Should move into BaseController and/or TransactionAction, but it's not
  // clear how, since the usage here flips the dependency
  protected def authorizeInTransaction(authority: Authority)(implicit r: RequestHeader) = {
    DB.withTransaction { implicit connection =>
      val adapter = new SquerylPostgreSqlAdapter()
      val session = new Session(connection, adapter)
      using(session) { // sets thread-local variable
        authorized(authority)
      }
    }
  }
  
  // Move this along with authorizeInTransaction
  def authorizedBodyParser[A](authority: Authority)(f: User => BodyParser[A]) = parse.using { implicit request =>
    authorizeInTransaction(authority) match {
      case Left(e) => parse.error(e)
      case Right(user) => f(user)
    }
  }

  // TODO: handle HEAD request
  def show(uuid: UUID) = Action(BodyParsers.parse.anyContent) { request =>
    Ok("ok")
  }

  /** Handle file upload and kick of documentSetCreationJob */
  def create(guid: UUID) = ActionInTransaction(authorizedFileUploadBodyParser(guid)) { authorizedCreate(guid)(_: Request[OverviewUpload], _: Connection) }

  private [controllers] def authorizedCreate(guid: UUID)(implicit request: Request[OverviewUpload], connection: Connection) = Ok
   
 /** Gets the guid and user info to the body parser handling the file upload */
  def authorizedFileUploadBodyParser(guid: UUID) = authorizedBodyParser(anyUser) { user => fileUploadBodyParser(user, guid) }

 
  def fileUploadBodyParser(user: User, guid: UUID): BodyParser[OverviewUpload] = BodyParser("File upload") { request =>
	fileUploadIteratee(user.id, guid, request)  
  }

  /** Abstract method for creating the Iteratee that handles the upload */
  protected def fileUploadIteratee(userId: Long, guid: UUID, requestHeader: RequestHeader): Iteratee[Array[Byte], Either[Result, OverviewUpload]]
}

/**
 * UploadController implementation that uses FileUploadIteratee
 */
object UploadController extends UploadController {

  def fileUploadIteratee(userId: Long, guid: UUID, requestHeader: RequestHeader): Iteratee[Array[Byte], Either[Result, OverviewUpload]] =
    FileUploadIteratee.store(userId, guid, requestHeader) 
}
 
