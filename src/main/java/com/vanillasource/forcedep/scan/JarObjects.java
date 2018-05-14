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

package com.vanillasource.forcedep.scan;

import com.vanillasource.forcedep.Objects;
import com.vanillasource.forcedep.Dependencies;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.function.Function;
import org.apache.log4j.Logger;

public final class JarObjects implements Objects {
   private static final Logger LOGGER = Logger.getLogger(JarObjects.class);
   private final File file;
   private final Function<InputStream, Objects> objectFactory;

   public JarObjects(File file, Function<InputStream, Objects> objectFactory) {
      this.file = file;
      this.objectFactory = objectFactory;
   }

   @Override
   public void analyze(Dependencies dependencies) {
      try {
         JarFile jarFile = new JarFile(file);
         new AggregateObjects(
               jarFile.stream()
               .filter(entry -> entry.getName().endsWith(".class"))
               .peek(entry -> LOGGER.debug("analyzing "+entry+" from jar file "+file))
               .map(uio(jarFile::getInputStream))
               .map(objectFactory)
               .collect(Collectors.toList())
         ).analyze(dependencies);
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

   // This sucks. Either support checked exceptions or don't Java!
   private static <T, R> Function<T, R> uio(IOFunction<T, R> function) {
      return t -> {
         try {
            return function.apply(t);
         } catch (IOException e) {
            throw new UncheckedIOException(e);
         }
      };
   }

   private interface IOFunction<T, R> {
      R apply(T t) throws IOException;
   }
}

