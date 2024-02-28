# cqf-tooling

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencds.cqf/tooling/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.opencds.cqf/tooling) [![Build Status](https://www.travis-ci.com/cqframework/cqf-tooling.svg?branch=master)](https://www.travis-ci.com/cqframework/cqf-tooling) [![project chat](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://chat.fhir.org/#narrow/stream/179220-cql)

Tooling for CQL and IG Authors

This tooling provides various useful tools for building CQFramework related content and implementation guides, including QUICK page generation, ModelInfo generation from StructureDefinitions, and ValueSet creation.

## Usage

Building this project requires Java 11+ and Maven 3.8+. The resulting jar is compatible with Java 11+.

Build the project with:

```bash
mvn package
```

Run a specific operation using (example running Accelerator Kit Processor):

```bash
cd tooling-cli 
```

```bash
mvn exec:java \
-Dexec.mainClass="org.opencds.cqf.tooling.cli.Main" \
-Dexec.args="-ProcessAcceleratorKit \
-pts=/Users/mutesasiramoses/Desktop/dev/HIV-DAK/cqf-tooling/input/WHO-UCN-HHS-SIA-2022.1-eng.xlsx \
-op=/Users/mutesasiramoses/Desktop/dev/HIV-DAK/cqf-tooling/output \
-s='hiv' \
-dep='HIV.A. Registration,HIV.B. HTS Visit,HIV.C. PrEP Visit,HIV.D. Care-Treatment Core,\
HIV.D.Care-Treatment Additional,HIV.D. TB-HIV,\
HIV.E-F. PMTCT,HIV.G. Diagnostics,HIV.H. Follow Up,HIV.I. Referral,HIV. Surveillance,HIV. Configuration'"
```
For running directly in VS code , add this to Launch.json

```bash
{
    // Use IntelliSense to learn about possible attributes.
    // Hover to view descriptions of existing attributes.
    // For more information, visit: https://go.microsoft.com/fwlink/?linkid=830387
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Run Java",
            "request": "launch",
            "mainClass": "org.opencds.cqf.tooling.cli.Main",
            "projectName": "tooling-cli",
            "args": [
                "-ProcessAcceleratorKit",
                "-op=/Users/mutesasiramoses/Desktop/dev/HIV-DAK/cqf-tooling/output",
                "-dep='HIV.A.Registration,HIV.B.HTSVisit,HIV.C.PrEPVisit,HIV.D.Care-TreatmentCore,HIV.D.Care-TreatmentAdditional,HIV.D.TB-HIV,HIV.E-F.PMTCT,HIV.G.Diagnostics,HIV.H.FollowUp,HIV.I.Referral,HIV.Surveillance,HIV.Configuration'",
                "-pts=/Users/mutesasiramoses/Desktop/dev/HIV-DAK/cqf-tooling/input/WHO-UCN-HHS-SIA-2022.1-eng.xlsx"
            ]
        }
    ]
}
```

Executable jars are produced by the CI system on Maven Central: [Download executable jar](https://oss.sonatype.org/service/local/artifact/maven/redirect?r=releases&g=org.opencds.cqf&a=tooling-cli&v=LATEST)

This can be run with `java -jar tooling-cli-2.0.0.jar -VsacXlsxToValueSetBatch`

Documentation of the various operations is provided in the [Main](tooling-cli/src/main/java/org/opencds/cqf/tooling/cli/Main.java) class.

## Commit Policy

All new development takes place on `<feature>` branches off `master`. Once feature development on the branch is complete, the feature branch is submitted to `master` as a PR. The PR is reviewed by maintainers and regression testing by the CI build occurs.

Changes to the `master` branch must be done through an approved PR. Delete branches after merging to keep the repository clean.

Merges to `master` trigger a deployment to the Maven Snapshots repositories. Once ready for a release, the `master` branch is updated with the correct version number and is tagged. Tags trigger a full release to Maven Central and a corresponding release to Github. Releases SHALL NOT have a SNAPSHOT version, nor any SNAPSHOT dependencies.

## Release Process

To release a new version of the tooling:
1. Update master to be a release version (and all the reviews, bug fixes, etc. that that requires)
   1. Regression test against IGs known to use CQF Tooling
2. Passed Travis Build = ready for release
3. Create a Github Release (which creates a tag at the current commit of master)
   1. Choose the "Auto-generate release notes" option
4. Travis does the release to Maven
   1. Ensure binaries are published to https://oss.sonatype.org/#view-repositories;public~browsestorage~org/opencds/cqf/tooling
5. Update master to vNext-SNAPSHOT
6. Close all issues included in the release

## Getting Help

Bugs and feature requests can be filed with [Github Issues](https://github.com/cqframework/cqf-tooling/issues).

The implementers are active on the official FHIR [Zulip chat for CQL](https://chat.fhir.org/#narrow/stream/179220-cql).

Inquires for commercial support can be directed to [info@alphora.com](info@alphora.com).

## Related Projects

[Clinical Quality Language](https://github.com/cqframework/clinical_quality_language) - Tooling in support of the CQL specification, including the CQL verifier/translator used in this project.

[CQL Support for Atom](https://atom.io/packages/language-cql) - Open source CQL IDE with syntax highlighting, linting, and local CQL evaluation.

[CQF Ruler](https://github.com/DBCG/cqf-ruler) - Integrates this project into the HAPI FHIR server, exposing some functionality as services.

## License

Copyright 2019+ Dynamic Content Group, LLC (dba Alphora)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
