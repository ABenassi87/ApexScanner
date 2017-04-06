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

import com.neowit.apex.symbols.SymbolKind

case class ClassNode(range: Range) extends ClassLike {
    override def nodeType: AstNodeType = ClassNodeType

    override def symbolKind: SymbolKind = SymbolKind.Class
}


/*
object ApexClass{
    def apply(ctx: ClassDefContext): ApexClass = {
        var annotationOpt = None
        val modifierSetBuilder = Set.newBuilder[Modifiers.Modifier]

        ctx.children.iterator().forEachRemaining{
            case elem: ClassOrInterfaceModifierContext =>

            case elem: ClassDeclarationContext =>
            case elem => throw new NotImplementedError("Unsupported element: " + elem)

        }
        ???
    }
}
*/