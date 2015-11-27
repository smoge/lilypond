+ SequenceableCollection {
	intervals {
		var array;
		array = [];
		this.doAdjacentPairs { |a, b| array = array.add(b - a) };
		^array;
	}
	offsets {
		var array;
		array = ([0] ++ this.integrate);
		^array;
	}
}

+ Integer {
	isAssignableDuration {
		var index, bool;
		index = this.asBinaryString.indexOf($1);
		if (index.isNil) { ^false };
		bool = this.asBinaryString[index..].contains("01").not;
		^bool;
	}
	//!!! TODO
	partitionUnassignableDuration {
	}
}

+ String {
	// generate incremental pathnames with date stamp (eg: "150911.0.ly", "150911.1.ly")
	*nextDateStampedPathname { |path, fileExt|
		var paths, dayStamp, index, outPath;
		path = path ?? { Platform.userHomeDir ++ "/" };
		if (path.last != $/) { path = path ++ "/" };
		paths = (path ++ "*").pathMatch;
		dayStamp = Date.getDate.dayStamp;
		index = paths.select { |each| each.contains(dayStamp) }.size;
		outPath = path ++ dayStamp ++ "." ++ index;
		if (fileExt.notNil) { outPath = outPath ++ "." ++ fileExt };
		^outPath;
	}
}

+ True {
	lpStr { ^"##t" }
}

+ False {
	lpStr { ^"##f" }
}