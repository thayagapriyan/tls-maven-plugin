/*
 * Verifies the plugin executed and logged the expected warning when the bucket doesn't exist.
 * This test proves the plugin wiring is correct without needing real AWS credentials.
 */
File buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log not found"

String log = buildLog.text
// The plugin must have run and logged a warning about the missing file
assert log.contains("fetch-tls-context") : "Plugin goal did not execute"
assert log.contains("continuing (failOnMissingFile=false)") || log.contains("Could not download") :
        "Expected a warning about download failure, but got: " + log

// The target file must NOT exist (download failed gracefully)
File target = new File(basedir, "target/classes/certificates/keystore.jks")
assert !target.exists() : "File should not have been written (no real S3 bucket)"
