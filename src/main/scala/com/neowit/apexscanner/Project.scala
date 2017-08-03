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

package com.neowit.apexscanner

import java.io.File
import java.nio.file._

import com.neowit.apexscanner.ast.{AstBuilder, AstBuilderResult, QualifiedName}
import com.neowit.apexscanner.extlib.CodeLibrary
import com.neowit.apexscanner.nodes.AstNode
import com.neowit.apexscanner.scanner.Scanner
import com.neowit.apexscanner.stdlib.impl.StdlibLocal

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Andrey Gavrikov 
  */
object Project {

    val defaultIsIgnoredPath: Path => Boolean = Scanner.defaultIsIgnoredPath
    def getClassOrTriggerMatcher(name: String): PathMatcher = {
        FileSystems.getDefault.getPathMatcher("""regex:(?i).*\b""" + name + """\.(cls|trigger)$""")
    }

    /**
      * using provided path try to find src (root) project folder
      * project root is usually same folder that contains package.xml file
      * @param path starting from provided path
      * @return
      */
    def findApexProjectRoot(path: Path): Option[Path] = {
        findApexProjectRoot(path, maxDepth = 10)
    }

    /**
      *
      * @param path start path
      * @param maxDepth if current path is not project root then maxDepth (if >=0) tells how many levels up we are
      *                 allowed to go up the file hierarchy
      * @return
      */
    @tailrec
    private def findApexProjectRoot(path: Path, maxDepth: Int): Option[Path] = {
        if (null == path || maxDepth < 0) {
            None
        } else if ( isProjectRoot(path) ) {
            Option(path)
        } else {
            // go up
            val parent = path.getParent
            findApexProjectRoot(parent, maxDepth - 1)
        }
    }

    private def isProjectRoot(path: Path): Boolean = {
        // check if path points to a folder that either has name "src" or contains file package.xml
        null != path && path.toFile.isDirectory && (path.endsWith("src") || new File(path.toString, "package.xml").canRead)
    }
}

/**
  * @param path must point to Apex project root, @see also Project.findApexProjectRoot(path)
  *             no check is made to ensure that provided path points to correct folder
  */
case class Project(path: Path)(implicit ex: ExecutionContext) extends CodeLibrary {
    private val astBuilder: AstBuilder = new AstBuilder(this)
    private val _externalLibraries = new collection.mutable.ListBuffer[CodeLibrary]

    private val _fileContentByPath = new mutable.HashMap[Path, VirtualDocument]()


    def addExternalLibrary(lib: CodeLibrary): Unit = {
        _externalLibraries += lib
    }
    def getExternalLibraries: List[CodeLibrary] = {
        _externalLibraries.toList
    }
    def loadExternalLibraries(): Unit = {
        _externalLibraries.foreach(_.load(this))
    }

    def saveFileContent(document: VirtualDocument): Future[Unit] = {
        _fileContentByPath += document.file -> document
        Future.successful(())
    }
    def getFileContent(file: Path): Option[VirtualDocument] = {
        _fileContentByPath.get(file)
    }
    def clearFileContent(file: Path): Future[Unit] = {
        _fileContentByPath -= file
        Future.successful(())
    }

    override def getName: String = "project"

    override def load(project: Project): CodeLibrary = this

    override def find(qualifiedName: QualifiedName): Option[AstNode] = getByQualifiedName(qualifiedName)

    override def isLoaded: Boolean = true

    override def getByQualifiedName(qualifiedName: QualifiedName): Option[AstNode] = {
        super.getByQualifiedName(qualifiedName) match {
            case nodeOpt @ Some(_) => nodeOpt
            case None => findInExternalLibrary(qualifiedName)
        }
    }

    def loadStdLib(): CodeLibrary = {
        val stdLib =
            getExternalLibraries.find(_.getName == "StdLib") match {
                case Some(_stdLib) => _stdLib
                case None => StdlibLocal(this)
            }
        stdLib.load(this)
    }

    private def findInExternalLibrary(qualifiedName: QualifiedName): Option[AstNode] = {
        for (lib <- getExternalLibraries) {
            if (!lib.isLoaded) {
                lib.load(this)
            }
            lib.find(qualifiedName) match {
                case Some(node) => return Option(node)
                case None =>
            }
        }
        None
    }

    /**
      * @param forceRebuild set to true in order to force AST re-build/re-cache for provided document
      * @return
      */
    def getAst(document: VirtualDocument, forceRebuild: Boolean = false): Future[Option[AstBuilderResult]] = {
        astBuilder.getAst(document) match {
          case Some(_ast) if !forceRebuild => Future.successful(Option(_ast))
          case _ =>
              // looks like AST for given file has not been built yet, let's fix it
              astBuilder.build(document).map { _ =>
                  astBuilder.getAst(document)
              }
        }
    }

    /**
      * check if given name corresponds to a valid apex file (.cls, .trigger) inside current project
      * if it does then return document instance
      * @param name potential document/file name (without extension), e.g. MyClass
      * @return
      */
    def getDocumentByName(name: String): Option[VirtualDocument] = {
        import scala.collection.JavaConverters._
        val rootPath = this.path
        val matcher = Project.getClassOrTriggerMatcher(name)
        val isIgnoredPath: Path => Boolean = Project.defaultIsIgnoredPath
        val paths = Files.walk(rootPath).iterator().asScala.filter(file => matcher.matches(file) && !isIgnoredPath(file))
        paths.toList.headOption match {
            case Some(foundPath) => Option(FileBasedDocument(foundPath))
            case None => None
        }
    }

}
