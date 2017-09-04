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

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.RuleNode

case class Position(line: Int, col: Int) extends Ordered[Position]{
    def isBefore(p: Position): Boolean = {
        isBefore(p.line, p.col)
    }
    def isBefore(otherLine: Int, otherCol: Int): Boolean = {
        line < otherLine ||
        line == otherLine && col < otherCol
    }
    def getDistance(p: Position): Distance = {
        if (this.line == p.line) {
            Distance(0, this.col - p.col)
        }
        val lines = this.line - p.line
        val cols = this.col - p.col
        Distance(lines, cols)
    }

    override def compare(that: Position): Int = {
        if (this.isBefore(that)) {
            -1
        } else if (this == that) {
            0
        } else {
            1
        }
    }
}
case class Distance(lines: Int, cols: Int)
object Distance {
    def min(d1: Distance, d2: Distance): Distance = {
        if (d1.lines == d2.lines) {
            if (d1.cols < d2.cols) d1 else d2
        } else {

            if (d1.lines < d2.lines) d1 else d2
        }
    }
}
object Position {
    val INVALID_LOCATION = Position(-1, -1)
    val FALLTHROUGH_LOCATION = Position(-2, -2)
}

/**
  *
  * @param start position where range starts
  * @param end position where range ends
  * @param offset if current range is relative then offset may be used to specify parent range
  *               and allow to find absolute position of current Range
  */
case class Range(start: Position, end: Position, offset: Position = Position(0, 0)) {
    def getDebugInfo: String = {
        val text =
            if (start != end) {
                s"${start.line}, ${start.col} - ${end.line}, ${end.col}"
            } else {
                s"${start.line}, ${start.col}"
            }
        "(" + text + ")"
    }

    /**
      * check if given location is inside current LocationInterval
      * @param location - line/col to check for inclusion
      * @return true if given location is inside current LocationInterval
      */
    def includesLocation(location: Position, ignoreOffset: Boolean): Boolean = {
        val startLine = if (ignoreOffset || offset.line < 1) start.line else (start.line - 1) + offset.line //-1 because lines start with 1
        val startCol = if (ignoreOffset) start.col else start.col + offset.col

        if (startLine > location.line || startLine == location.line && startCol > location.col) {
            return false
        }

        val endLine = if (ignoreOffset) end.line else end.line + offset.line
        val endCol = if (ignoreOffset) end.col else end.col + offset.col

        if (endLine < location.line || endLine == location.line && endCol < location.col) {
            return false
        }
        true
    }
}

object Range {
    val INVALID_LOCATION = Range(Position.INVALID_LOCATION, Position.INVALID_LOCATION)
    val FALLTHROUGH_LOCATION = Range(Position.FALLTHROUGH_LOCATION, Position.FALLTHROUGH_LOCATION)

    def apply(node: RuleNode): Range = {
        FALLTHROUGH_LOCATION
    }

    def apply(ctx: ParserRuleContext, offsetPosition: Position): Range = {
        val startToken = ctx.getStart
        val endToken = ctx.getStop
        val startPosition = Position(startToken.getLine, startToken.getCharPositionInLine)
        // when calculating endPosition take into account length of stop token
        val endPosition = Position(endToken.getLine, endToken.getCharPositionInLine + endToken.getStopIndex - endToken.getStartIndex + 1)
        Range(
            start = startPosition,
            end = endPosition,
            offset = offsetPosition
        )

    }
    def apply(ctx: ParserRuleContext): Range = {
        apply(ctx, Position(0, 0))
    }

    def apply(node: org.antlr.v4.runtime.tree.TerminalNode, offsetPosition: Position): Range = {
        Range(
            start = Position(node.getSymbol.getLine, node.getSymbol.getCharPositionInLine),
            end = Position(node.getSymbol.getLine, node.getSymbol.getCharPositionInLine + node.getSymbol.getText.length -1),
            offset = offsetPosition
        )
    }
    def apply(node: org.antlr.v4.runtime.tree.TerminalNode): Range = {
        apply(node, Position(0, 0))
    }
}

