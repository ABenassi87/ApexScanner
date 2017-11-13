/*
 * Copyright (c) 2017 Andrey Gavrikov.
 *
 * This file is part of https://github.com/neowit/apexscanner
 *
 *  This file is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This file is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.neowit.apexscanner.server.protocol

import java.nio.file.Path

import com.neowit.apexscanner.Project
import com.neowit.apexscanner.server.handlers._
import com.neowit.apexscanner.server.protocol.messages.MessageParams.InitializeParams
import com.neowit.apexscanner.server.protocol.messages._
import com.typesafe.scalalogging.LazyLogging

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Andrey Gavrikov 
  */
trait LanguageServer extends LazyLogging {
    implicit val ex: ExecutionContext

    private val _projectByPath = new mutable.HashMap[Path, Project]()
    def shutdown(): Unit

    def sendNotification(notification: NotificationMessage): Unit

    def initialiseProject(params: InitializeParams): Future[Either[String, Project]] = Future {
        params.rootUri.path match {
            case Some(_) => // can not use params.rootUri.path directly because it may not point to project root
                initialiseProjectImpl(params) match {
                    case Right(project) =>
                        _projectByPath += project.path -> project
                        Right(project)
                    case err => err
                }
            case None =>
                val err = "Failed to create project - InitializeParams.rootUri.path is missing: "
                logger.error(err)
                Left(err)
        }
    }

    protected def initialiseProjectImpl(params: InitializeParams): Either[String, Project]

    /**
      * given the file path, try to determine its project from the list of initialized projects
      * @param path path to a file inside project
      * @return
      */
    @tailrec
    final def getProject(path: Path): Option[Project] = {
        if (null != path) {
            _projectByPath.get(path) match {
              case Some(_project) => Option(_project)
              case None => getProject(path.getParent)
            }
        } else {
            None
        }
    }

    def process(message: Message)(implicit ex: ExecutionContext): Future[Either[Unit, ResponseMessage]] = {

        val response: Option[Future[Either[ResponseError, ResponseMessage]]] =
            message match {
                case m @ RequestMessage(id, "initialize", initializeParams, _) =>
                    val handler = new InitializeHandler()
                    val msg = handler.handle(this, m)
                    Option(msg)
                case RequestMessage(_, "shutdown", None, _) =>
                    shutdown()
                    None
                case m @ RequestMessage(id, "textDocument/completion", params, _) =>
                    val handler = new CompletionHandler()
                    val msg = handler.handle(this, m)
                    Option(msg)
                case m @ RequestMessage(id, "textDocument/definition", params, _) =>
                    val handler = new DefinitionHandler()
                    val msg = handler.handle(this, m)
                    Option(msg)
                case NotificationMessage("$/cancelRequest", _, _) =>
                    //A processed notification message must not send a response back. They work like events.
                    //TODO
                    None
                case m @ NotificationMessage("initialized", _, _) =>
                    //initialized is a notification and does not require a response
                    None
                case m @ NotificationMessage("textDocument/didOpen", params, _) =>
                    None
                case m @ NotificationMessage("textDocument/didChange", params, _) =>
                    val handler = new DidChangeHandler()
                    handler.handle(this, m)
                    None
                case m @ NotificationMessage("textDocument/didSave", params, _) =>
                    val handler = new DidSaveHandler()
                    handler.handle(this, m)
                    None
                case NotificationMessage(_, _, _) =>
                    //A processed notification message must not send a response back. They work like events.
                    //TODO
                    None
                case _ =>
                    val error = ResponseError(ErrorCodes.MethodNotFound, s"Message not supported: $message")
                    Option(Future.successful(Left(error)))
            }

        /*
        response  match {
          case Some(Left(error)) =>
              Option(ResponseMessage(id = 0, result = None, error = Option(error)))
          case Some(Right(msg)) => Option(msg)
          case None => None
        }
        */
        response  match {
            case Some(futureResult) =>
                futureResult.map{
                    case Left(error) =>
                        Right(ResponseMessage(id = 0, result = None, error = Option(error)))
                    case Right(msg) => Right(msg)
                }
            case None => Future.successful(Left(()))
        }

    }

}
