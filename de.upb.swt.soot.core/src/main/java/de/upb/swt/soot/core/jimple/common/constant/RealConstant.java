package de.upb.swt.soot.core.jimple.common.constant;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 1997-1999 Raja Vallee-Rai
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */

import javax.annotation.Nonnull;

/**
 * Base type for floating point constants.
 *
 * @see DoubleConstant
 * @see FloatConstant
 */
public interface RealConstant<R extends RealConstant<R>> extends NumericConstant<R> {

  /**
   * Performs the indicated floating point comparison. For {@code NaN} comparisons {@code -1} is
   * returned.
   *
   * @param constant the value to compare with
   * @return {@code 0} if values are equal, {@code 1} if passed value less, or {@code -1} if passed
   *     value greater. When any of the values is {@code NaN} methodRef returns {@code -1}.
   */
  @Nonnull
  IntConstant cmpl(@Nonnull R constant);

  /**
   * Performs the indicated floating point comparison. For {@code NaN} comparisons {@code 1} is
   * returned.
   *
   * @param constant the value to compare with
   * @return {@code 0} if values are equal, {@code 1} if passed value less, or {@code -1} if passed
   *     value greater. When any of the values is {@code NaN} methodRef returns {@code 1}.
   */
  @Nonnull
  IntConstant cmpg(@Nonnull R constant);
}
