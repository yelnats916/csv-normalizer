# CSV-normalizer

How to run:
Install Java 8
```
cd csv-normalizer
java -cp target/csv-normalizer-1.0-SNAPSHOT-jar-with-dependencies.jar com.normalizer.Normalizer sample-with-broken-utf8.csv outputFileName.csv
```
Sample-with-broken-utf8.csv is the input file and outputFileName.csv is the name of the newly generated output file

If there are issues running, recreate the jar using Maven (dependencies are listed in pom.xml file)
Install Maven then run:
```
mvn clean
```

Note: If there are dropped lines due to invalid/unparseable data, they will be appended to a newly created output file named "errorFile"
