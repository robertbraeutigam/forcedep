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
import com.vanillasource.forcedep.transform.*;
import com.vanillasource.forcedep.d3.D3Dependencies;
import java.util.List;
import static java.util.Arrays.asList;
import java.util.stream.Collectors;
import java.io.File;

public final class Main {
   private final String analysisName;
   private final String outputFileName;
   private final boolean active;
   private final int size;
   private final List<String> inputFileNames;
   private final List<String> whitelist;
   private final List<String> blacklist;

   public Main(String analysisName, String outputFileName, boolean active, int size, List<String> inputFileNames, List<String> whitelist, List<String> blacklist) {
      this.outputFileName = outputFileName;
      this.inputFileNames = inputFileNames;
      this.active = active;
      this.size = size;
      this.analysisName = analysisName;
      this.whitelist = whitelist;
      this.blacklist = blacklist;
   }

   public void run() throws Exception {
      Objects objects = new AggregateObjects(inputFileNames
            .stream()
            .map(File::new)
            .map(file -> new JarObjects(file, AsmClass::new))
            .collect(Collectors.toList()));

      try (Dependencies dependencies =
            new FilteredDependencies(whitelist, blacklist,
               new OverrideDependencies(
                  new ExistingObjectsDependencies(
                     new MergedAnonymousClassesDependencies(
                        new MergedPrivateMethodsDependencies(
                           new MergedLambdaDependencies(
                              new UniqueDependencies(
                                 new D3Dependencies(analysisName, new File(outputFileName), active, size))))))))) {
         objects.analyze(dependencies);
      }
   }

   public static void main(String[] args) throws Exception {
      Options options = new Options();
      options.addOption(Option.builder("o").longOpt("output").hasArg().argName("FILENAME").desc("The output file to write resulting HTML into").build());
      options.addOption(Option.builder("n").longOpt("name").hasArg().argName("NAME").desc("The analysis name reported on resulting HTML").build());
      options.addOption(Option.builder("w").longOpt("whitelist").hasArg().argName("REGEXP").desc("Whitelist to filter object FQNs").build());
      options.addOption(Option.builder("s").longOpt("size").hasArg().argName("SIZE").desc("Radius of one object in pixels.").build());
      options.addOption(Option.builder("b").longOpt("blacklist").hasArg().argName("REGEXP").desc("Blacklist to filter object FQNs").build());
      options.addOption(Option.builder("x").desc("Initialized simulation in stopped state").build());
      CommandLineParser parser = new DefaultParser();
      CommandLine cmdLine = parser.parse(options, args);
      new Main(
            cmdLine.getOptionValue('n', generateAnalysisName(cmdLine.getArgList())),
            cmdLine.getOptionValue('o', "output.html"),
            !cmdLine.hasOption('x'),
            Integer.valueOf(cmdLine.getOptionValue('s', "8")),
            cmdLine.getArgList(),
            cmdLine.getOptionValues('w')==null?asList(".*"):asList(cmdLine.getOptionValues('w')),
            cmdLine.getOptionValues('b')==null?asList():asList(cmdLine.getOptionValues('b')))
         .run();
   }

   private static String generateAnalysisName(List<String> inputFileNames) {
      return inputFileNames.stream()
         .map(File::new)
         .map(File::getName)
         .collect(Collectors.joining(", "));
   }
}
