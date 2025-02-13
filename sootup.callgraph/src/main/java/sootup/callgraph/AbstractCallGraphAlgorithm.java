package sootup.callgraph;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 2019-2020 Christian Brüggemann, Ben Hermann, Markus Schmidt and others
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Method;
import sootup.core.model.SootClass;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.signatures.MethodSubSignature;
import sootup.core.typehierarchy.TypeHierarchy;
import sootup.core.types.ClassType;
import sootup.core.views.View;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.types.JavaClassType;

/**
 * The AbstractCallGraphAlgorithm class is the super class of all call graph algorithmen. It
 * provides basic methods used in all call graph algorithm. It is abstract since it has no
 * implemented functionality to resolve method calls because it is decided by the applied call graph
 * algorithm
 */
public abstract class AbstractCallGraphAlgorithm implements CallGraphAlgorithm {

  private static final Logger logger = LoggerFactory.getLogger(AbstractCallGraphAlgorithm.class);

  @Nonnull protected final View<? extends SootClass<?>> view;
  @Nonnull protected final TypeHierarchy typeHierarchy;

  protected AbstractCallGraphAlgorithm(
      @Nonnull View<? extends SootClass<?>> view, @Nonnull TypeHierarchy typeHierarchy) {
    this.view = view;
    this.typeHierarchy = typeHierarchy;
  }

  /**
   * This method starts the construction of the call graph algorithm. It initializes the needed
   * objects for the call graph generation and calls processWorkList method.
   *
   * @param view the view contains all needed class files.
   * @param entryPoints a list of method signatures that will be added to the work list in the call
   *     graph generation.
   * @return the complete constructed call graph starting from the entry methods.
   */
  @Nonnull
  final CallGraph constructCompleteCallGraph(
      View<? extends SootClass<?>> view, List<MethodSignature> entryPoints) {
    MutableCallGraph cg = new GraphBasedCallGraph();

    Deque<MethodSignature> workList = new ArrayDeque<>(entryPoints);
    Set<MethodSignature> processed = new HashSet<>();

    processWorkList(view, workList, processed, cg);
    return cg;
  }

  /**
   * Processes all entries in the <code>workList</code>, skipping those present in <code>processed
   *  </code>, adding call edges to the graph. Newly discovered methods are added to the <code>
   *  workList</code> and processed as well. <code>cg</code> is updated accordingly. The method
   * postProcessingMethod is called after a method is processed in the <code>worklist</code>.
   *
   * @param view it contains the classes.
   * @param workList it contains all method that have to be processed in the call graph generation.
   *     This list is filled in the execution with found call targets in the call graph algorithm.
   * @param processed the list of processed method to only process the method once.
   * @param cg the call graph object that is filled with the found methods and call edges.
   */
  final void processWorkList(
      View<? extends SootClass<?>> view,
      Deque<MethodSignature> workList,
      Set<MethodSignature> processed,
      MutableCallGraph cg) {
    while (!workList.isEmpty()) {
      MethodSignature currentMethodSignature = workList.pop();
      if (processed.contains(currentMethodSignature)) continue;

      if (!cg.containsMethod(currentMethodSignature)) cg.addMethod(currentMethodSignature);

      Stream<MethodSignature> invocationTargets =
          resolveAllCallsFromSourceMethod(view, currentMethodSignature);

      invocationTargets.forEach(
          t -> {
            if (!cg.containsMethod(t)) cg.addMethod(t);
            if (!cg.containsCall(currentMethodSignature, t)) {
              cg.addCall(currentMethodSignature, t);
              workList.push(t);
            }
          });
      processed.add(currentMethodSignature);

      postProcessingMethod(view, currentMethodSignature, workList, cg);
    }
  }

  /**
   * This method resolves all calls from a given source method. resolveCall is called for each
   * invoke statement in the body of the source method that is implemented in the corresponding call
   * graph algorithm.
   *
   * @param view it contains all classes.
   * @param sourceMethod this signature is used to access the statements contained method body of
   *     the specified method
   * @return a stream containing all resolved callable method signatures by the given source method
   */
  @Nonnull
  Stream<MethodSignature> resolveAllCallsFromSourceMethod(
      View<? extends SootClass<?>> view, MethodSignature sourceMethod) {
    SootMethod currentMethodCandidate =
        view.getClass(sourceMethod.getDeclClassType())
            .flatMap(c -> c.getMethod(sourceMethod.getSubSignature()))
            .orElse(null);
    if (currentMethodCandidate == null) return Stream.empty();

    if (currentMethodCandidate.hasBody()) {
      return currentMethodCandidate.getBody().getStmtGraph().nodes().stream()
          .filter(Stmt::containsInvokeExpr)
          .flatMap(s -> resolveCall(currentMethodCandidate, s.getInvokeExpr()));
    } else {
      return Stream.empty();
    }
  }

  /**
   * searches the method object in the given hierarchy
   *
   * @param view it contains all classes
   * @param sig the signature of the searched method
   * @param <T> the generic type of the searched method object
   * @return the found method object, or null if the metod was not found.
   */
  final <T extends Method> T findMethodInHierarchy(
      @Nonnull View<? extends SootClass<?>> view, @Nonnull MethodSignature sig) {
    Optional<? extends SootClass<?>> optSc = view.getClass(sig.getDeclClassType());

    if (optSc.isPresent()) {
      SootClass<?> sc = optSc.get();

      List<ClassType> superClasses = typeHierarchy.superClassesOf(sc.getType());
      Set<ClassType> interfaces = typeHierarchy.implementedInterfacesOf(sc.getType());
      superClasses.addAll(interfaces);

      for (ClassType superClassType : superClasses) {
        Optional<? extends SootClass<?>> superClassOpt = view.getClass(superClassType);
        if (superClassOpt.isPresent()) {
          SootClass<?> superClass = superClassOpt.get();
          Optional<? extends SootMethod> methodOpt = superClass.getMethod(sig.getSubSignature());
          if (methodOpt.isPresent()) {
            return (T) methodOpt.get();
          }
        }
      }
      logger.warn(
          "Could not find \""
              + sig.getSubSignature()
              + "\" in "
              + sig.getDeclClassType().getClassName()
              + " and in its superclasses");
    } else {
      logger.trace("Could not find \"" + sig.getDeclClassType() + "\" in view");
    }
    return null;
  }

  /**
   * This method enables optional post processing of a method in the call graph algorithm
   *
   * @param view it contains classes and the type hierarchy.
   * @param sourceMethod the processed method
   * @param workList the current worklist that might be extended
   * @param cg the current cg that might be extended
   */
  public void postProcessingMethod(
      View<? extends SootClass<?>> view,
      MethodSignature sourceMethod,
      @Nonnull Deque<MethodSignature> workList,
      @Nonnull MutableCallGraph cg) {
    // is only implemented if it is needed in the call graph algorithm
  }

  @Nonnull
  @Override
  public CallGraph addClass(@Nonnull CallGraph oldCallGraph, @Nonnull JavaClassType classType) {
    MutableCallGraph updated = oldCallGraph.copy();

    SootClass<?> clazz = view.getClassOrThrow(classType);
    Set<MethodSignature> newMethodSignatures =
        clazz.getMethods().stream().map(Method::getSignature).collect(Collectors.toSet());

    if (newMethodSignatures.stream().anyMatch(oldCallGraph::containsMethod)) {
      throw new IllegalArgumentException("CallGraph already contains methods from " + classType);
    }

    // Step 1: Add edges from the new methods to other methods
    Deque<MethodSignature> workList = new ArrayDeque<>(newMethodSignatures);
    Set<MethodSignature> processed = new HashSet<>(oldCallGraph.getMethodSignatures());
    processWorkList(view, workList, processed, updated);

    // Step 2: Add edges from old methods to methods overridden in the new class
    List<ClassType> superClasses = typeHierarchy.superClassesOf(classType);
    Set<ClassType> implementedInterfaces = typeHierarchy.implementedInterfacesOf(classType);
    Stream<ClassType> superTypes =
        Stream.concat(superClasses.stream(), implementedInterfaces.stream());

    Set<MethodSubSignature> newMethodSubSigs =
        newMethodSignatures.stream()
            .map(MethodSignature::getSubSignature)
            .collect(Collectors.toSet());

    superTypes
        .map(view::getClassOrThrow)
        .flatMap(superType -> superType.getMethods().stream())
        .map(Method::getSignature)
        .filter(
            superTypeMethodSig -> newMethodSubSigs.contains(superTypeMethodSig.getSubSignature()))
        .forEach(
            overriddenMethodSig -> {
              //noinspection OptionalGetWithoutIsPresent (We know this exists)
              MethodSignature overridingMethodSig =
                  clazz.getMethod(overriddenMethodSig.getSubSignature()).get().getSignature();

              for (MethodSignature callingMethodSig : updated.callsTo(overriddenMethodSig)) {
                updated.addCall(callingMethodSig, overridingMethodSig);
              }
            });

    return updated;
  }

  /**
   * The method iterates over all classes present in view, and finds method with name main and
   * SourceType - Library. This method is used by initialize() method used for creating call graph
   * and the call graph is created by considering the main method as an entry point.
   *
   * <p>The method throws an exception if there is no main method in any of the classes or if there
   * are more than one main method.
   *
   * @return - MethodSignature of main method.
   */
  public MethodSignature findMainMethod() {
    Set<SootClass<?>> classes = new HashSet<>(); /* Set to track the classes to check */
    for (SootClass<?> aClass : view.getClasses()) {
      if (!aClass.isLibraryClass()) {
        classes.add(aClass);
      }
    }

    Collection<SootMethod> mainMethods = new HashSet<>(); /* Set to store the methods */
    for (SootClass<?> aClass : classes) {
      for (SootMethod method : aClass.getMethods()) {
        if (method.isStatic()
            && method
                .getSignature()
                .equals(
                    JavaIdentifierFactory.getInstance()
                        .getMethodSignature(
                            aClass.getType(),
                            "main",
                            "void",
                            Collections.singletonList("java.lang.String[]")))) {
          mainMethods.add(method);
        }
      }
    }

    if (mainMethods.size() > 1) {
      throw new RuntimeException(
          "There are more than 1 main method present.\n Below main methods are found: \n"
              + mainMethods
              + "\n initialize() method can be used if only one main method exists. \n You can specify these main methods as entry points by passing them as parameter to initialize method.");
    } else if (mainMethods.size() == 0) {
      throw new RuntimeException(
          "No main method is present in the input programs. initialize() method can be used if only one main method exists in the input program and that should be used as entry point for call graph. \n Please specify entry point as a parameter to initialize method.");
    }

    return mainMethods.stream().findFirst().get().getSignature();
  }

  /**
   * This methods resolves the possible targets of a given invoke expression. The results are
   * dependable of the applied call graph algorithm. therefore, it is abstract.
   *
   * @param method the method object that contains the given invoke expression in the body.
   * @param invokeExpr it contains the call which is resolved.
   * @return a stream of all reachable method signatures defined by the applied call graph
   *     algorithm.
   */
  @Nonnull
  abstract Stream<MethodSignature> resolveCall(SootMethod method, AbstractInvokeExpr invokeExpr);
}
