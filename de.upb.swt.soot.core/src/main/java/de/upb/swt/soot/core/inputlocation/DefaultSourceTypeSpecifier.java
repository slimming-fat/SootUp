package de.upb.swt.soot.core.inputlocation;
/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 2020 Markus Schmidt
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
import de.upb.swt.soot.core.SourceTypeSpecifier;
import de.upb.swt.soot.core.model.SourceType;
import de.upb.swt.soot.core.types.ClassType;
import javax.annotation.Nonnull;

/**
 * Implements a very basic version of {@link SourceTypeSpecifier} which tells the type of a class by
 * checking if it is a language build-in class.
 *
 * @author Markus Schmidt
 */
public class DefaultSourceTypeSpecifier implements SourceTypeSpecifier {

  private static final DefaultSourceTypeSpecifier INSTANCE = new DefaultSourceTypeSpecifier();

  /** Singleton to get an Instance of this SourceTypeSpecifier */
  public static DefaultSourceTypeSpecifier getInstance() {
    return INSTANCE;
  }

  private DefaultSourceTypeSpecifier() {}

  @Nonnull
  public SourceType sourceTypeFor(ClassType type) {
    if (type.isBuiltInClass()) {
      return SourceType.Library;
    }
    return SourceType.Application;
  }
}
