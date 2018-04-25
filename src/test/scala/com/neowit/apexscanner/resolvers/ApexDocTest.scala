/*
 * Copyright (c) 2018 Andrey Gavrikov.
 * this file is part of tooling-force.com application
 * https://github.com/neowit/tooling-force.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.neowit.apexscanner.resolvers

import java.nio.file.{Path, Paths}

import com.neowit.apexscanner.{Project, TextBasedDocument, VirtualDocument}
import com.neowit.apexscanner.VirtualDocument.DocumentId
import com.neowit.apexscanner.antlr.CaretUtils
import com.neowit.apexscanner.ast.AstBuilderResult
import com.neowit.apexscanner.nodes.HasApexDoc
import com.neowit.apexscanner.scanner.actions.{ActionContext, FindSymbolActionType}
import org.scalatest.FunSuite

import scala.util.Random

/**
  * Created by Andrey Gavrikov 
  */
class ApexDocTest extends FunSuite {
    private val projectPath: Path = Paths.get(System.getProperty("java.io.tmpdir"))
    private class TestDocument(text: String, fileName: String) extends TextBasedDocument(text, fileOpt = Option(Paths.get(fileName)), offset = None) {
        override def getId: DocumentId = fileName
    }
    var _projectWithStdLib: Option[Project] = None
    private def getProject: Project = {
        val _project = Project(projectPath)
        _project
    }
    private def loadDocument(project: Project, document: VirtualDocument): Option[AstBuilderResult] = {
        project.getAst(document, forceRebuild = true)
    }
    private def findApexDoc(text: String, documentName: String = "test"): Option[String] = {
        val project = getProject
        loadDocument(project, new TestDocument(text, documentName))
        val caretInDocument = CaretUtils.getCaret(text, Paths.get(documentName))
        val resultNodes =
        project.getAst(caretInDocument.document, forceRebuild = true) match {
            case Some(result) =>
                val actionContext = ActionContext("ApexDocTest-" + Random.nextString(5), FindSymbolActionType)
                val finder = new AscendingDefinitionFinder(actionContext)
                finder.findDefinition(result.rootNode, caretInDocument.position)
            case _ =>
                Seq.empty
        }
        assert(resultNodes.nonEmpty, "Expected to find non empty result")
        resultNodes.head match {
            case typeDefinition: HasApexDoc =>
                typeDefinition.getApexDoc.map(_.text)
            case _ =>
                fail("Failed to locate correct node.")
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////

    test("apex-doc: class header") {
        val text =
            """
              |/**
              | * this is apex doc 1
              | */
              |class ApexDocTester {
              | private void test() {
              |     ApexDocTes<CARET>ter;
              | }
              |
              |}
            """.stripMargin
        //val resultNodes = findDefinition(text).futureValue
        val apexDoc = findApexDoc(text)
        assert(apexDoc.nonEmpty, "Expected to find non empty apex doc")
        assert(apexDoc.exists(_.contains("this is apex doc 1")), "Wrong apex doc returned")

    }
}