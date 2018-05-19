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

package com.vanillasource.forcedep.d3;

import com.vanillasource.forcedep.Dependencies;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import org.jtwig.JtwigTemplate;
import org.jtwig.JtwigModel;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Saves dependencies directly to a D3 graph.
 */
public final class D3Dependencies implements Dependencies {
   private final JsonArray nodes = new JsonArray();
   private final JsonArray links = new JsonArray();

   public void writeTo(File file) throws IOException {
      try (FileOutputStream output = new FileOutputStream(file)) {
         writeTo(output);
      }
   }

   public void writeTo(OutputStream output) {
      JtwigTemplate template = JtwigTemplate.classpathTemplate("/com/vanillasource/forcedep/d3/force-template.html");
      JtwigModel model = JtwigModel.newModel()
         .with("nodes", nodes.toString(WriterConfig.PRETTY_PRINT))
         .with("links", links.toString(WriterConfig.PRETTY_PRINT));
      template.render(model, output);
   }

   @Override
   public Dependencies.Object object(String objectFqn, String... superObjectFqns) {
      return new Dependencies.Object() {
         @Override
         public Dependencies.Method method(String methodName) {
            JsonObject jsonMethod = new JsonObject();
            jsonMethod.add("id", methodId(objectFqn, methodName));
            jsonMethod.add("name", methodName);
            nodes.add(jsonMethod);
            return new Dependencies.Method() {
               @Override
               public void call(String calledObjectFqn, String calledMethodName) {
                  JsonObject jsonCall = new JsonObject();
                  jsonCall.add("source", methodId(objectFqn, methodName));
                  jsonCall.add("target", methodId(calledObjectFqn, calledMethodName));
                  links.add(jsonCall);
               }

               @Override
               public void reference(String objectsFqn, String fieldName) {
                  // TODO
               }

               @Override
               public void close() {
               }
            };
         }

         @Override
         public void close() {
         }
      };
   }

   private static String methodId(String objectFqn, String methodName) {
      return objectFqn+"."+methodName+"()";
   }
}


