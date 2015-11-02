package com.overviewdocs.messages

import com.overviewdocs.models.FileGroup

/** Background tasks that must be serialized on a document set.
  *
  * These commands are sent from the web server to the worker. All messages
  * are:
  *
  * * Write-only: There are no return values.
  * * Backed up: The web server has stored the data elsewhere (in the
  *   database), so that if the worker is dead it will infer the message the
  *   next time it starts up.
  * * Serialized: on a given document set, only one message will be processed
  *   at a time. Messages are processed in FIFO order.
  */
object DocumentSetCommands {
  sealed trait Command { val documentSetId: Long }

  /** Empty all GroupedFileUploads from the given FileGroup, add the resulting
    * Documents to the DocumentSet, then delete the FileGroup.
    *
    * Stored as a DocumentSetCreationJob.
    */
  case class AddDocumentsFromFileGroup(fileGroup: FileGroup) extends Command {
    override val documentSetId = fileGroup.addToDocumentSetId.get
  }

  /** Delete a DocumentSet and all associated information.
    *
    * Stored in the database as document_set.deleted.
    */
  case class DeleteDocumentSet(documentSetId: Long) extends Command

  /** Complete all computations surrounding a DocumentSetCreationJob as soon as
    * possible, and then delete the DocumentSetCreationJob.
    *
    * This Command is different from the rest: it is *not serialized*. As soon
    * as the broker receives this Command, it will forward a cancel message to
    * all workers and purge the associated Job from its own memory (if it's
    * pending).
    *
    * Stored in the database as document_set_creation_job.state = Cancelled
    */
  case class CancelJob(documentSetId: Long, jobId: Long) extends Command

  /** Completes all computations surrounding an AddDocumentsFromFileGroup job
    * as soon as possible, then deletes the AddDocumentsFromFileGroup.
    *
    * This Command is different from the rest: it is *not serialized*. As soon
    * as the broker receives this Command, it will forward a cancel message to
    * all workers and purge the associated Job from its own memory (if it's
    * pending).
    *
    * Stored in the database as file_group.deleted = true.
    */
  case class CancelAddDocumentsFromFileGroup(
    documentSetId: Long,
    fileGroupId: Long
  ) extends Command
}