# PIT run 1 (developer, outside the agent): stopped part-way

- Started 2026-11-13 07:11, stopped by the developer at 14:23 (computer shutdown). 1 thread, alone on the machine.
- Results so far: mutations-partial-raw.xml (761 complete results; the last record is cut off). 44 classes seen.
- Interim: 644 KILLED, 45 TIMED_OUT, 47 SURVIVED, 26 NO_COVERAGE (762 incl. the cut-off record), out of 948 mutants.
- classes-done.txt: 42 classes treated as finished. RequestLogFilter and UserService were being worked on when stopped, so they are re-run.

## Resume (run 2)

Run PIT only on classes not in classes-done.txt, by excluding them (plus ContactGroupsApplication, as pom.xml does):

    EXCL=$(paste -sd, classes-done.txt),app.contactgroups.ContactGroupsApplication
    ./mvnw -B org.pitest:pitest-maven:mutationCoverage -DexcludedClasses="$EXCL"

Merge: run-1 results for classes in classes-done.txt + all run-2 results. Check: the merged total must be 948 mutants;
if it is less, a class in classes-done.txt was not finished and must be re-run.
