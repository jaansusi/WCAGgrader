<html>
<?php
require 'data.php';
$mysqli = new mysqli($server, $user, $pass, $db, 3306);
if ($mysqli->connect_errno) {
	echo "Failed to connect to MYSQL: (" . $mysqli->connect_errno . ") " . $mysqli->connect_errno; 
} else {
	$mysqli->set_charset('utf8');
	$results = $mysqli->query("SELECT * FROM access_lint ORDER BY time DESC");
	/**
	for ($rowNr = 0; $rowNr <= $result->num_rows - 1; $rowNr++) {
		$result->data_seek($rowNr);
		$row = $result->fetch_assoc();
		echo $row;
	
	}
	**/
	foreach ($results as $result) {
		var_dump ($result);
		echo '<br /><br />';
	}	
}
?>
</html>
