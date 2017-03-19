/*
 * Copyright (c) 2017 Andrey Gavrikov.
 *
 * This file is part of https://github.com/neowit/apexscanner
 *
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.neowit.apex.scanner

import java.io.FileInputStream
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}

import com.neowit.apex.scanner.antlr.{ApexcodeLexer, ApexcodeParser, CaseInsensitiveInputStream}
import org.antlr.v4.runtime.{BaseErrorListener, CommonTokenStream, RecognitionException, Recognizer}

import scala.collection.mutable

object SyntaxChecker{

    private[SyntaxChecker] def emptyOnEachResult(file: Path, res: CheckSyntaxResult): Unit = Unit

    private val ignoredDirs = Set("resources_unpacked", "Referenced Packages")
    def defaultIsIgnoredPath(path: Path): Boolean = {
        val isDirectory = Files.isDirectory(path)
        val fileName = path.getName(path.getNameCount-1).toString
        if (isDirectory) {
            return ignoredDirs.contains(fileName)
        } else {
            //regular file
            if (
                (fileName.endsWith(".cls") || fileName.endsWith(".trigger"))
                    && !fileName.contains("__") // exclude classes with namespace <Namespace>__classname.cls, they do not have apex code
            ) {
                return false // not ignored file
            }
        }
        true
    }
}
class SyntaxChecker {
    import SyntaxChecker._

    /**
      * Check syntax in files residing in specified path/location
      * @param path file or folder with eligible apex files to check syntax
      * @param isIgnoredPath - provide this function if path points to a folder and certain paths inside need to be ignored
      * @param onEachResult - provide this function if additional action is required when result for each individual file is available
      * @return
      */
    def check(path: Path,
              isIgnoredPath: Path => Boolean = defaultIsIgnoredPath,
              onEachResult: (Path, CheckSyntaxResult) => Unit = emptyOnEachResult): Seq[CheckSyntaxResult] = {
        val fileListBuilder = List.newBuilder[Path]

        val apexFileVisitor = new SimpleFileVisitor[Path]() {
            override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
                if (!attrs.isDirectory && !isIgnoredPath(file) ) {
                    fileListBuilder += file
                }
                FileVisitResult.CONTINUE
            }

            override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
                if (isIgnoredPath(dir)) {
                    FileVisitResult.SKIP_SUBTREE
                } else {
                    super.preVisitDirectory(dir, attrs)
                }
            }
        }
        Files.walkFileTree( path, apexFileVisitor)
        val files = fileListBuilder.result()

        val fileNameSetBuilder = Set.newBuilder[String]
        val resultBuilder = Seq.newBuilder[CheckSyntaxResult]


        files.foreach{ file =>
            val lexer = getLexer(file)
            val fileName = file.getName(file.getNameCount-1).toString
            fileNameSetBuilder += fileName
            val tokens = new CommonTokenStream(lexer)
            val parser = new ApexcodeParser(tokens)
            val errorBuilder = Seq.newBuilder[SyntaxError]
            parser.addErrorListener(new SyntaxCheckerErrorListener(file, errorBuilder))

            // run actual scan
            parser.compilationUnit()

            val errors = errorBuilder.result()
            val res = CheckSyntaxResult(file, errors)
            onEachResult(file, res)
            resultBuilder += res
        }
        resultBuilder.result()
    }

    /**
      * default case insensitive ApexCode lexer
      * @param file - file to parse
      * @return case insensitive ApexcodeLexer
      */
    def getLexer(file: Path): ApexcodeLexer = {
        //val input = new ANTLRInputStream(new FileInputStream(file))
        val input = new CaseInsensitiveInputStream(new FileInputStream(file.toFile))
        val lexer = new ApexcodeLexer(input)
        lexer
    }
}

case class SyntaxError(offendingSymbol: scala.Any,
                       line: Int,
                       charPositionInLine: Int,
                       msg: String)

class SyntaxCheckerErrorListener(file: Path,
                                 errorBuilder: mutable.Builder[SyntaxError, Seq[SyntaxError]]) extends BaseErrorListener {

    override def syntaxError(recognizer: Recognizer[_, _],
                             offendingSymbol: scala.Any,
                             line: Int, charPositionInLine: Int,
                             msg: String,
                             e: RecognitionException): Unit = {
        //super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e)
        val error = SyntaxError(offendingSymbol, line, charPositionInLine, msg)
        errorBuilder += error
        //assert(false, "\n" + file.toString + s"\n=> ($line, $charPositionInLine): " + msg)
    }
}
