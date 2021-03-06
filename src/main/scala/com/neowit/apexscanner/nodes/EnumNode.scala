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

package com.neowit.apexscanner.nodes

import com.neowit.apexscanner.ast.QualifiedName
import com.neowit.apexscanner.symbols.{Symbol, SymbolKind}

case class EnumNode(override val name: Option[String], range: Range ) extends ClassLike {
    override def nodeType: AstNodeType = EnumNodeType

    override def symbolKind: SymbolKind = SymbolKind.Enum

    override def getValueType: Option[ValueType] = {
        qualifiedName.map(name => ValueTypeEnum(name))
    }

    override protected def resolveDefinitionImpl(actionContext: com.neowit.apexscanner.scanner.actions.ActionContext): Option[AstNode] = Option(this)

    override def extendsNode: Option[ExtendsNode] = None

    override def supportsInnerClasses: Boolean = false

    /**
      * get super class of current class/interface
      *
      * @return
      */
    override def getSuperClassOrInterface: Option[ClassLike] = None

    override def implements: Seq[ImplementsInterfaceNode] = Seq.empty

    override def getSymbolsOfKind(kind: SymbolKind): Seq[Symbol] = {
        val symbols: Seq[Symbol] =
            kind match {
                case SymbolKind.Method => findChildrenInAst(_.nodeType == MethodNodeType).map(_.asInstanceOf[Symbol])
                case SymbolKind.Variable => findChildrenInAst(_.nodeType == EnumConstantNodeType).map(_.asInstanceOf[Symbol])
                case SymbolKind.Property => findChildrenInAst(_.nodeType == EnumConstantNodeType).map(_.asInstanceOf[Symbol])
                case _ => Seq.empty
            }
        symbols
    }
}

object EnumNode {
    /**
      * add standard ENUM method - values()
      * @param node parent node
      */
    def addStandardMethods(node: EnumNode): Unit = {
        //values(): List<Enum-Type>
        node.getValueType.map(valueType =>
            node.addChildToAst(
                MethodNode.createMethodNode(
                    methodName = "values",
                    methodIsStatic = false,
                    methodIsAbstract = false,
                    methodReturnType = ValueTypeComplex(QualifiedName(Array("List")), Seq(valueType)),
                    parameterTypes = Array.empty,
                    methodApexDoc = Option("This method returns the values of the Enum as a list of the same Enum type.")
                ))

        )
        ()
    }
}