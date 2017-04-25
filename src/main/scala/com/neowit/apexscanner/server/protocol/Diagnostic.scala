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

import com.neowit.apexscanner.nodes.Range

/**
  * Represents a diagnostic, such as a compiler error or warning.
  * Diagnostic objects are only valid in the scope of a resource.
  */
trait Diagnostic {
    /**
      * The range at which the message applies.
      */
    val range: Range

    /**
      * The diagnostic's severity. Can be omitted. If omitted it is up to the
      * client to interpret diagnostics as error, warning, info or hint.
      */
    val severity: DiagnosticSeverity

    /**
      * The diagnostic's code. Can be omitted.
      */
    val code: String

    /**
      * A human-readable string describing the source of this
      * diagnostic, e.g. 'typescript' or 'super lint'.
      */
    val source: Option[String]

    /**
      * The diagnostic's message.
      */
    val message: String
}
