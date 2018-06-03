/**
 * Copyright (C) 2018 VanillaSource
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.vanillasource.forcedep.jvm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Handle;
import com.vanillasource.forcedep.Objects;
import com.vanillasource.forcedep.Dependencies;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import org.apache.log4j.Logger;

/**
 * A single class analyzed by the ASM library.
 */
public final class AsmClass implements Objects {
   private static final Logger LOGGER = Logger.getLogger(AsmClass.class);
   private final ClassReader classReader;

   public AsmClass(InputStream bytes) {
      try {
         this.classReader = new ClassReader(bytes);
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

   @Override
   public void analyze(Dependencies dependencies) {
      classReader.accept(new AsmClassVisitor(dependencies), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
   }

   private static class AsmClassVisitor extends ClassVisitor {
      private final Dependencies dependencies;
      private String objectFqn;
      private String[] superObjectFqns;
      private boolean anonymous = false;
      private boolean pureInterface;
      private Dependencies.Object cachedObject;
      private Set<String> fields = new HashSet<>();

      public AsmClassVisitor(Dependencies dependencies) {
         super(Opcodes.ASM6);
         this.dependencies = dependencies;
      }

      @Override
      public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
         LOGGER.debug("visiting class: "+name+", signature: "+signature);
         List<String> superObjectFqns = new ArrayList<>();
         if (superName != null) {
            superObjectFqns.add(fqn(superName));
         }
         for (String interfaceName: interfaces) {
            superObjectFqns.add(fqn(interfaceName));
         }
         this.pureInterface = (access & Opcodes.ACC_INTERFACE) != 0;
         this.objectFqn = fqn(name);
         this.superObjectFqns = superObjectFqns.toArray(new String[] {});
      }

      private Dependencies.Object object() {
         if (cachedObject == null) {
            this.cachedObject = dependencies.object(objectFqn, anonymous, pureInterface, superObjectFqns);
         }
         return cachedObject;
      }

      @Override
      public void visitInnerClass(String name, String outerName, String innerName, int access) {
         LOGGER.debug("visiting inner class: "+name+", outer name: "+outerName+", inner name: "+innerName);
         if (fqn(name).equals(objectFqn) && innerName == null) {
            anonymous = true;
         }
      }

      @Override
      public void visitOuterClass(String owner, String name, String descriptor) {
         LOGGER.debug("visiting outer class: "+name+", owner: "+owner);
      }

      @Override
      public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
         LOGGER.debug("visiting field: "+name);
         if (!fields.contains(name)) {
            fields.add(name);
            object().field(name);
         }
         return null;
      }

      private static String fqn(String classloaderName) {
         return classloaderName.replaceAll("/", ".");
      }

      @Override
      public void visitEnd() {
         object().close();
      }

      @Override
      public MethodVisitor visitMethod(int callerAccess, String callerName, String callerDescription, String callerSignature, String[] callerExceptions) {
         LOGGER.debug("visiting method: "+callerName+", signature: "+callerSignature);
         Dependencies.Method method = object().method(callerName, (callerAccess&Opcodes.ACC_PRIVATE)!=0);
         return new MethodVisitor(Opcodes.ASM6) {
            @Override
            public void visitMethodInsn(int calleeOpcode, String calleeOwner, String calleeName, String calleeDescriptor, boolean calleeIsInterface) {
               LOGGER.debug("visiting call: "+calleeName+", owner: "+calleeOwner+", from method: "+callerName);
               method.call(fqn(calleeOwner), calleeName);
            }

            @Override
            public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
               if (bootstrapMethodHandle.getName().equals("metafactory")) {
                  Handle handle = (Handle) bootstrapMethodArguments[1];
                  LOGGER.debug("visiting lambda call: "+handle.getName()+", owner: "+handle.getOwner());
                  method.call(fqn(handle.getOwner()), handle.getName());
               }
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
               LOGGER.debug("visit field access: "+name+", owner: "+owner+", descriptor: "+descriptor);
               method.reference(fqn(owner), name);
               if (fqn(owner).equals(objectFqn) && !fields.contains(name)) {
                  // Owner of fields sometimes is the current object, not superclass, 
                  // so make these fields seem like part of this object too
                  fields.add(name);
                  object().field(name);
               }
            }

            @Override
            public void visitEnd() {
               method.close();
            }
         };
      }
   }
}

