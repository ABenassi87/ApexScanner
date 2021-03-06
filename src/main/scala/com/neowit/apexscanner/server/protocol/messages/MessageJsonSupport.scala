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

package com.neowit.apexscanner.server.protocol.messages

import com.neowit.apexscanner.nodes.{Location, Position, Range}
import com.neowit.apexscanner.server.protocol._
import com.neowit.apexscanner.server.protocol.messages.MessageParams._
import io.circe._
import io.circe.generic.semiauto._
import cats.syntax.either._
import com.neowit.apexscanner.symbols.SymbolKind
/**
  * Created by Andrey Gavrikov 
  */
trait MessageJsonSupport {
    implicit val RequestMessageDecoder: Decoder[RequestMessage] = deriveDecoder
    implicit val NotificationMessageDecoder: Decoder[NotificationMessage] = deriveDecoder
    implicit val NotificationMessageEncoder: Encoder[NotificationMessage] = deriveEncoder

    implicit val CompletionOptionsEncoder: Encoder[CompletionOptions] = deriveEncoder
    implicit val SignatureHelpOptionsEncoder: Encoder[SignatureHelpOptions] = deriveEncoder
    implicit val CodeLensOptionsEncoder: Encoder[CodeLensOptions] = deriveEncoder
    implicit val DocumentOnTypeFormattingOptionsEncoder: Encoder[DocumentOnTypeFormattingOptions] = deriveEncoder
    implicit val ExecuteCommandOptionsEncoder: Encoder[ExecuteCommandOptions] = deriveEncoder
    implicit val SaveOptionsEncoder: Encoder[SaveOptions] = deriveEncoder
    implicit val TextDocumentSyncOptionsEncoder: Encoder[TextDocumentSyncOptions] = deriveEncoder
    implicit val ServerCapabilitiesEncoder: Encoder[ServerCapabilities] = deriveEncoder

    implicit val ResponseErrorEncoder: Encoder[ResponseError] = deriveEncoder
    implicit val ResponseMessageEncoder: Encoder[ResponseMessage] = deriveEncoder

    /*
    implicit val DocumentUriEncoder: Encoder[DocumentUri] = new Encoder[DocumentUri] {
        final def apply(a: DocumentUri): Json = a.uri.asJson
    }
    */
    implicit val DocumentUriEncoder: Encoder[DocumentUri] = Encoder.encodeString.contramap[DocumentUri](_.uri)
    implicit val DocumentUriDecoder: Decoder[DocumentUri] = Decoder.decodeString.emap { str =>
        Either.catchNonFatal(DocumentUri(str)).leftMap(t => "DocumentUri")
    }
    implicit val VSCodeUriDecoder: Decoder[VSCodeUri] = deriveDecoder

    implicit val WorkspaceClientCapabilitiesDecoder: Decoder[WorkspaceClientCapabilities] = deriveDecoder
    implicit val TextDocumentClientCapabilitiesDecoder: Decoder[TextDocumentClientCapabilities] = deriveDecoder
    implicit val ClientCapabilitiesDecoder: Decoder[ClientCapabilities] = deriveDecoder
    implicit val TextDocumentDecoder: Decoder[TextDocument] = deriveDecoder
    implicit val TextDocumentIdentifierDecoder: Decoder[TextDocumentIdentifier] = deriveDecoder
    implicit val InitializeParamsDecoder: Decoder[InitializeParams] = deriveDecoder
    implicit val DidSaveParamsDecoder: Decoder[DidSaveParams] = deriveDecoder
    implicit val CompletionParamsDecoder: Decoder[TextDocumentPositionParams] = deriveDecoder
    implicit val DocumentSymbolParamsDecoder: Decoder[DocumentSymbolParams] = deriveDecoder

    implicit val PositionDecoder: Decoder[Position] =
        Decoder.forProduct2("line", "character")(Position.apply)
    implicit val PositionEncoder: Encoder[Position] =
        Encoder.forProduct2("line", "character")(p => (p.line, p.col))

    //implicit val PositionEncoder: Encoder[Position] = deriveEncoder
    //implicit val PositionDecoder: Decoder[Position] = deriveDecoder
    //implicit val PositionEncoder: Encoder[Position] =
    //    Encoder.forProduct2("line", "col")(p => (p.line, p.col))

    implicit val RangeEncoder: Encoder[Range] = deriveEncoder

    implicit val DiagnosticSeverityEncoder: Encoder[DiagnosticSeverity] = Encoder.encodeInt.contramap[DiagnosticSeverity](_.code)
    //implicit val DiagnosticSeverityEncoder: Encoder[DiagnosticSeverity] =
    //    Encoder.forProduct1("code")(s => s.code )

    implicit val DiagnosticEncoder: Encoder[Diagnostic] = deriveEncoder
    implicit val PublishDiagnosticsParamsEncoder: Encoder[PublishDiagnosticsParams] = deriveEncoder

    implicit val SymbolKindEncoder: Encoder[SymbolKind] = Encoder.encodeInt.contramap[SymbolKind](_.code)
    implicit val LocationEncoder: Encoder[Location] =
        Encoder.forProduct2("uri", "range")(l =>
            (DocumentUri(l.path), l.range)
        )
    implicit val SymbolInformationParamsEncoder: Encoder[SymbolInformation] = deriveEncoder
    implicit val CompletionItemEncoder: Encoder[CompletionItem] = deriveEncoder

    implicit val TextDocumentContentChangeEventDecoder: Decoder[TextDocumentContentChangeEvent] = deriveDecoder
    implicit val VersionedTextDocumentIdentifierDecoder: Decoder[VersionedTextDocumentIdentifier] = deriveDecoder
    implicit val DidChangeTextDocumentParamsDecoder: Decoder[DidChangeTextDocumentParams] = deriveDecoder
    //implicit val ExecuteCommandArgumentsDecoder: Decoder[ExecuteCommandArguments] = deriveDecoder
    implicit val ExecuteCommandParamsDecoder: Decoder[ExecuteCommandParams] = deriveDecoder

    def toDocumentUri(json: Json): DocumentUri = {
        if (json.isString) {
            json.as[String] match {
                case Right(str) =>
                    json.as[DocumentUri] match {
                        case Right(uri) =>
                            uri
                        case Left(err) =>
                            throw new IllegalArgumentException(s"Failed to parse Uri. '$str'. " + err.message)

                    }
                case Left(err) => throw new IllegalArgumentException("Failed to parse Uri. " + err.message)
            }
        } else {
                json.as[VSCodeUri] match {
                    case Right(uri) =>
                        DocumentUri(uri.external)
                    case Left(err) =>
                        throw new IllegalArgumentException("Failed to parse VSCodeUri. " + err.message)
                }
        }


    }

    implicit val NotificationMessageParamsEncoder: Encoder[NotificationMessageParams] =
        Encoder.forProduct2("type", "message")(p => (p.`type`.code, p.message))
}
