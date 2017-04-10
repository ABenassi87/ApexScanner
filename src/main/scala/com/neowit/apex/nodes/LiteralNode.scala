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

package com.neowit.apex.nodes

import com.neowit.apex.ast.{AstVisitor, QualifiedName}
import com.neowit.apex.scanner.antlr.ApexcodeParser._
import org.antlr.v4.runtime.tree.TerminalNode

/**
  * Created by Andrey Gavrikov 
  */
object LiteralNode {
    private def getStandardNamespace(literalType: Int): QualifiedName = {
        literalType match {
            case IntegerLiteral => QualifiedName(Array("System", "Integer"))
            case StringLiteral => QualifiedName(Array("System", "String"))
            case FloatingPointLiteral => QualifiedName(Array("System", "Decimal"))
            case BooleanLiteral => QualifiedName(Array("System", "Boolean"))
            //TODO add SoslLiteral & SoqlLiteral
            case x => throw new NotImplementedError(s"Support for literal of type $x is not implemented")
        }
    }
}
case class LiteralNode(literalType: Int, valueTerminal: TerminalNode, range: Range) extends AstNode with HasTypeDefinition {
    import LiteralNode._

    override def nodeType: AstNodeType = LiteralNodeType
    def value: String = valueTerminal.getText

    override protected def resolveDefinitionImpl(visitor: AstVisitor): Option[AstNode] = {
        val stdLibOpt = getProject.map(_.getStandardLibrary)
        stdLibOpt match {
          case Some(stdLib) => stdLib.findChild(getStandardNamespace(literalType))

          case None => None
        }
    }

}





