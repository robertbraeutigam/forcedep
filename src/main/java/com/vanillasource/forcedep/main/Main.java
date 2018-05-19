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

package com.vanillasource.forcedep.main;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import com.vanillasource.forcedep.Objects;
import com.vanillasource.forcedep.Dependencies;
import com.vanillasource.forcedep.jvm.AsmClass;
import com.vanillasource.forcedep.scan.JarObjects;
import com.vanillasource.forcedep.scan.AggregateObjects;
import com.vanillasource.forcedep.transform.ExistingObjectsDependencies;
import com.vanillasource.forcedep.transform.OverrideDependencies;
import com.vanillasource.forcedep.d3.D3Dependencies;
import java.util.List;
import java.util.stream.Collectors;
import java.io.File;

public final class Main {
   private final String outputFileName;
   private final List<String> inputFileNames;

   public Main(String outputFileName, List<String> inputFileNames) {
      this.outputFileName = outputFileName;
      this.inputFileNames = inputFileNames;
   }

   public void run() throws Exception {
      Objects objects = new AggregateObjects(inputFileNames
            .stream()
            .map(File::new)
            .map(file -> new JarObjects(file, AsmClass::new))
            .collect(Collectors.toList()));

      D3Dependencies d3Dependencies = new D3Dependencies();
      Dependencies dependencies = new OverrideDependencies(
            new ExistingObjectsDependencies(d3Dependencies));

      objects.analyze(dependencies);

      d3Dependencies.writeTo(new File(outputFileName));
   }

   public static void main(String[] args) throws Exception {
      Options options = new Options();
      options.addOption(Option.builder("o").longOpt("output").hasArg().argName("FILENAME").desc("The output file to write resulting HTML into").build());
      CommandLineParser parser = new DefaultParser();
      CommandLine cmdLine = parser.parse(options, args);
      new Main(cmdLine.getOptionValue('o', "output.html"), cmdLine.getArgList()).run();
   }
}
