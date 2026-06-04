/*
 * Verifies that the file was actually downloaded from S3 and written to the output directory.
 */
File buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build.log not found"

String log = buildLog.text
assert log.contains("Downloaded") : "Expected 'Downloaded' log line indicating success"
assert log.contains("bytes)") : "Expected byte count in log"

// The target file must exist and be non-empty
File target = new File(basedir, "target/classes/certificates/test-keystore.jks")
assert target.exists() : "Downloaded file not found at expected path"
assert target.length() > 0 : "Downloaded file is empty"
