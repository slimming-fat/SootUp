package de.upb.soot.inputlocation;

import categories.Java8Test;
import de.upb.soot.DefaultIdentifierFactory;
import de.upb.soot.IdentifierFactory;
import de.upb.soot.frontends.AbstractClassSource;
import de.upb.soot.frontends.ClassProvider;
import de.upb.soot.frontends.asm.AsmJavaClassProvider;
import de.upb.soot.types.JavaClassType;
import java.util.Collection;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.mockito.internal.matchers.GreaterOrEqual;
import org.mockito.internal.matchers.LessOrEqual;

/*-
 * #%L
 * Soot
 * %%
 * Copyright (C) 07.06.2018 Manuel Benz
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

/** @author Manuel Benz created on 07.06.18 */
@Category(Java8Test.class)
public abstract class AbstractAnalysisInputLocationTest {

  protected static final int CLASSES_IN_JAR = 25;
  private IdentifierFactory identifierFactory;
  private ClassProvider classProvider;

  @Before
  public void setUp() {
    identifierFactory = DefaultIdentifierFactory.getInstance();
    classProvider = createClassProvider();
  }

  protected IdentifierFactory getIdentifierFactory() {
    return identifierFactory;
  }

  protected ClassProvider getClassProvider() {
    return classProvider;
  }

  protected ClassProvider createClassProvider() {
    return new AsmJavaClassProvider();
  }

  protected void testClassReceival(
      AbstractAnalysisInputLocation ns, JavaClassType sig, int minClassesFound) {
    testClassReceival(ns, sig, minClassesFound, -1);
  }

  protected void testClassReceival(
      AbstractAnalysisInputLocation ns,
      JavaClassType sig,
      int minClassesFound,
      int maxClassesFound) {
    final Optional<? extends AbstractClassSource> clazz = ns.getClassSource(sig);

    Assert.assertTrue(clazz.isPresent());
    Assert.assertEquals(sig, clazz.get().getClassType());

    final Collection<? extends AbstractClassSource> classSources =
        ns.getClassSources(getIdentifierFactory());
    Assert.assertNotNull(classSources);
    Assert.assertFalse(classSources.isEmpty());
    Assert.assertThat(classSources.size(), new GreaterOrEqual<>(minClassesFound));
    if (maxClassesFound != -1) {
      Assert.assertThat(classSources.size(), new LessOrEqual<>(maxClassesFound));
    }
  }
}