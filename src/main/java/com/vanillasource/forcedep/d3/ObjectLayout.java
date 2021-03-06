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

import com.eclipsesource.json.JsonObject;
import java.util.Map;
import java.util.HashMap;

public final class ObjectLayout {
   private final int width = 1024;
   private final int objectSize;
   private final int distance;
   private final int classWidth;
   private final int classHeight;
   private final int classesInRow;
   private int nextPositionIndex = 0;
   private Map<String, Position> objectPosition = new HashMap<>();

   public ObjectLayout(int objectSize) {
      this.objectSize = objectSize;
      this.distance = objectSize * 30 / 6;
      this.classWidth = 5 * distance;
      this.classHeight = 5 * distance;
      this.classesInRow = width / classWidth;
   }

   public void layoutObject(String objectFqn, JsonObject json) {
      objectPosition
         .computeIfAbsent(objectFqn, k -> new Position(nextPositionIndex++))
         .layout(json);
   }

   private class Position {
      private final int index;
      private int slot = 0;

      public Position(int index) {
         this.index = index;
      }

      public void layout(JsonObject json) {
         int x = 100 + (index % classesInRow) * classWidth + (slot % 3) * distance;
         int y = 100 + (index / classesInRow) * classHeight + (slot / 3) * distance;
         json.add("initialX", x);
         json.add("initialY", y);
         slot++;
      }
   }
}

