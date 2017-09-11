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

package com.neowit.apexscanner.completion

import com.neowit.apexscanner.{Project, TextBasedDocument, TokenBasedDocument, VirtualDocument}
import com.neowit.apexscanner.antlr.{ApexParserUtils, ApexcodeLexer, SoqlLexer, SoqlParserUtils}
import com.neowit.apexscanner.nodes.Position
import com.typesafe.scalalogging.LazyLogging
import org.antlr.v4.runtime._

/**
  * Created by Andrey Gavrikov
  * given caret position in Document - try to find its Scope/Type
  */
object CaretScopeFinder extends LazyLogging {

    def findCaretToken(caret: CaretInDocument, tokenStream: CommonTokenStream): Option[Token] = {
        tokenStream.fill()
        var i = 0
        val tokens = tokenStream
        var token: Token = tokens.get(i)
        while (caret.isAfter(token) && Token.EOF != token.getType) {
            i += 1
            token = tokens.get(i)
        }
        if (caret.isInside(token) || caret.isBefore(token)) {
            Option(token)
        } else {
            None
        }
    }

    def findFixerToken(tokenStream: CommonTokenStream): Option[Token] = {
        tokenStream.fill()
        var i = 0
        val tokens = tokenStream
        var token: Token = tokens.get(i)
        while (SoqlLexer.FIXER_TOKEN != token.getType && Token.EOF != token.getType) {
            i += 1
            token = tokens.get(i)
        }
        if (SoqlLexer.FIXER_TOKEN == token.getType) {
            Option(token)
        } else {
            None
        }
    }

    /**
      * insert or replace token in caret position with FIXER_TOKEN
      * @return updated document (if caret found)
      */
    private def injectFixerToken(caret: CaretInDocument): VirtualDocument = {
        val lexer = new ApexcodeLexer(caret.document.getCharStream)
        val tokens = new CommonTokenStream(lexer)
        val rewriter = new TokenStreamRewriter(tokens)
        val fixerTokenText = lexer.getVocabulary.getSymbolicName(ApexcodeLexer.FIXER_TOKEN)

        findCaretToken(caret, tokens) match {
            case Some(_caretToken) if _caretToken.getText.isEmpty || !ApexParserUtils.isWordToken(_caretToken)=>
                rewriter.insertBefore(_caretToken, fixerTokenText)
            case Some(_caretToken) if _caretToken.getText.nonEmpty && ApexParserUtils.isWordToken(_caretToken) =>
                rewriter.replace(_caretToken.getTokenIndex, fixerTokenText)
            case _ => // TODO
        }

        val fixedDocument = TextBasedDocument(rewriter.getText, caret.document.fileOpt, caret.document.offset)
        fixedDocument
    }

    private def injectFixerTokenInSoql(caretInApexDocument: CaretInDocument, caretTokenInApex: Token): VirtualDocument = {
        val soqlDocumentOffset = Position(caretTokenInApex)/*.addCol(1)*/// +1 to compensate the fact that both offset and SOQL share same start symbol
        val caretPositionInSoql = Position.toRelativePosition(caretInApexDocument.position, Option(soqlDocumentOffset))
        val caret = new CaretInDocument(
            caretPositionInSoql,
            TokenBasedDocument(
                caretTokenInApex,
                fileOpt = None,
                offset = Option(soqlDocumentOffset)
            )
        )
        val soqlDocument = TextBasedDocument(caretTokenInApex.getText, fileOpt = None, Option(soqlDocumentOffset))
        val lexer = new SoqlLexer(soqlDocument.getCharStream)
        val tokens = new CommonTokenStream(lexer)
        val rewriter = new TokenStreamRewriter(tokens)
        val fixerTokenText = lexer.getVocabulary.getSymbolicName(SoqlLexer.FIXER_TOKEN)

        findCaretToken(caret, tokens) match {
            case Some(caretToken) if caretToken.getText.isEmpty || !ApexParserUtils.isWordToken(caretToken)=>
                rewriter.insertBefore(caretToken, fixerTokenText)
            case Some(caretToken) if caretToken.getText.nonEmpty && ApexParserUtils.isWordToken(caretToken) =>
                rewriter.replace(caretToken.getTokenIndex, fixerTokenText)
            case _ => // TODO
        }
        val fixedSoqlDocument = TextBasedDocument(rewriter.getText, caret.document.fileOpt, soqlDocument.offset)
        fixedSoqlDocument
    }

    private def replaceTokenText(document: VirtualDocument, tokenToReplace: Token, newTextOpt: Option[String]): String = {
        newTextOpt match {
            case Some(newText) =>
                val lexer = new ApexcodeLexer(document.getCharStream)
                val tokens = new CommonTokenStream(lexer)
                tokens.fill()
                val rewriter = new TokenStreamRewriter(tokens)
                rewriter.replace(tokenToReplace.getTokenIndex, newText)
                rewriter.getText()
            case None =>
                document.getTextContent.getOrElse("")
        }
    }
}

class CaretScopeFinder(project: Project) extends LazyLogging {
    import CaretScopeFinder._

    def findCaretScope(caretInOriginalDocument: CaretInDocument): Option[FindCaretScopeResult] = {
        val lexer = new ApexcodeLexer(caretInOriginalDocument.document.getCharStream)
        val tokens = new CommonTokenStream(lexer)
        findCaretToken(caretInOriginalDocument, tokens) match {
            case Some(caretTokenInApex) if ApexcodeLexer.SoqlLiteral == caretTokenInApex.getType =>
                // looks like caret is inside SOQL literal
                findCaretScopeInSoql(caretInOriginalDocument, caretTokenInApex)
            case Some(caretTokenInApex) =>
                findCaretScopeInApex(caretInOriginalDocument, caretTokenInApex)
            case None =>
                None
        }
    }

    private def findCaretScopeInApex(caretInOriginalDocument: CaretInDocument, caretTokenInOriginalDocument: Token): Option[FindCaretScopeResult] = {
        // alter original document by injecting FIXER_TOKEN
        val fixedDocument = injectFixerToken(caretInOriginalDocument)
        val caret = new CaretInFixedDocument(caretInOriginalDocument.position, fixedDocument, caretInOriginalDocument.document)
        val (parser, tokenStream) = ApexParserUtils.createParserWithCommonTokenStream(caret)

        findCaretToken(caret, tokenStream) match {
            case Some(caretToken) =>
                //now when we found token corresponding caret position try to understand context
                //collectCandidates(caret, caretToken, parser)
                val resolver = new CaretExpressionResolver(project)
                val tokens = parser.getTokenStream
                resolver.resolveCaretScope(caret, caretToken, tokens) match {
                    case caretScopeOpt @ Some( CaretScope(_, _)) =>
                        Option(FindCaretScopeResult(caretScopeOpt, caretToken))
                    case _  =>
                        Option(FindCaretScopeResult(None, caretToken))
                }
            case None =>
                None
        }
    }

    private def findCaretScopeInSoql(caretInOriginalDocument: CaretInDocument, caretTokenInApex: Token): Option[FindCaretScopeResult] = {
        val fixedSoqlDocument = injectFixerTokenInSoql(caretInOriginalDocument, caretTokenInApex)
        val (parser, tokenStream) = SoqlParserUtils.createParserWithCommonTokenStream(fixedSoqlDocument)

        findFixerToken(tokenStream) match { // fixer token corresponds to "caret" token position
            case Some(caretTokenInSoqlDocument) =>
                // here we need to provide caret with "fixed" Apex document but caretToken from SOQL document
                // with absolute location inside Apex Document
                val fixedApexDocumentText = replaceTokenText(caretInOriginalDocument.document, caretTokenInApex, fixedSoqlDocument.getTextContent)
                val fixedApexDocument = TextBasedDocument(fixedApexDocumentText, caretInOriginalDocument.document.fileOpt, offset = None)

                // using position of FIXER_TOKEN in SOQL Document and position of SOQL Literal in Apex Document
                // calculate "caret" position in fixed document
                val caretPositionInFixedApexDocument = Position.toAbsolutePosition(Position(caretTokenInSoqlDocument), Option(Position(caretTokenInApex)))
                val caret = new CaretInFixedDocument(caretPositionInFixedApexDocument, fixedApexDocument, caretInOriginalDocument.document)

                // generate new token which is based on the absolute Position of caret token in SOQL document inside Apex Document
                val caretToken = CommonTokenFactory.DEFAULT.create(caretTokenInSoqlDocument.getType, caretTokenInSoqlDocument.getText)
                caretToken.setLine(caretPositionInFixedApexDocument.line)
                caretToken.setCharPositionInLine(caretPositionInFixedApexDocument.col)

                //now when we found token corresponding to caret position try to understand context
                //collectCandidates(caret, caretToken, parser)
                val resolver = new CaretExpressionResolver(project)
                val tokens = parser.getTokenStream
                resolver.resolveCaretScope(caret, caretToken, tokens) match {
                    case caretScopeOpt @ Some( CaretScope(_, _)) =>
                        Option(FindCaretScopeResult(caretScopeOpt, caretToken))
                    case _  =>
                        Option(FindCaretScopeResult(None, caretToken))
                }
            case None =>
                None
        }
    }
}
