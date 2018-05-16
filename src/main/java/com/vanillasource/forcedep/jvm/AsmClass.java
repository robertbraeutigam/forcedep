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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import com.vanillasource.forcedep.Objects;
import com.vanillasource.forcedep.Dependencies;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.ArrayList;
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
      private Dependencies.Object object;

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
         this.object = dependencies.object(fqn(name), superObjectFqns.toArray(new String[] {}));
      }

      private static String fqn(String classloaderName) {
         return classloaderName.replaceAll("/", ".");
      }

      @Override
      public MethodVisitor visitMethod(int callerAccess, String callerName, String callerDescription, String callerSignature, String[] callerExceptions) {
         LOGGER.debug("visiting method: "+callerName+", signature: "+callerSignature);
         Dependencies.Method method = object.method(callerName);
         return new MethodVisitor(Opcodes.ASM6) {
            public void visitMethodInsn(int calleeOpcode, String calleeOwner, String calleeName, String calleeDescriptor, boolean calleeIsInterface) {
               method.call(fqn(calleeOwner), calleeName);
            }
         };
      }
   }
}

