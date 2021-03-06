/* ---------------------------------------------------------------------------------------------------------------
LP_Config.bin;
LP_Config.dir;
LP_Config.version;
LP_Config.language;
--------------------------------------------------------------------------------------------------------------- */
LP_Config {
	classvar <>bin="/Applications/LilyPond.app/Contents/Resources/bin/lilypond", <>dir;
	classvar >version, <>language="english";
	*initClass {
		dir = Platform.userAppSupportDir ++ "/lilypond/";
		if (File.exists(dir).not) { unixCmd("mkdir" + dir.shellQuote) };
	}
	*version {
		var str;
		if (this.binExists.not) { ^error("Lilypond binary not found at" + this.bin) };
		^version ?? {
			str = (LP_Config.bin + "--version").unixCmdGetStdOut;
			str.copyRange(*[str.findRegexp("\\s[0-9]")[0][0]+1, str.find("\n")-1]);
		};
	}
	*binExists {
		^File.exists(this.bin);
	}
}

LP_File {
	var music, path;
	var <headerBlock, <layoutBlock, <paperBlock, <scoreContextBlock;
	var <defaultPaperSize=#['a4', 'landscape'], <>globalStaffSize=16;
	*new { |music|
		^super.new.init(music);
	}
	init { |argMusic|
		music = argMusic;
		headerBlock = LP_HeaderBlock();
		layoutBlock = LP_LayoutBlock();
		paperBlock = LP_PaperBlock();
		scoreContextBlock = LP_ContextBlock('Score');
	}
	defaultPaperSize_ { |paper='a4', orientation='landscape'|
		defaultPaperSize = [paper, orientation];
	}
	initDefaults {
		headerBlock.tagline_(false);
		layoutBlock.addContextBlock(scoreContextBlock);
		if (paperBlock.margin.isNil) { paperBlock.margin_(20, 20, 20, 20) };
		if (paperBlock.indent.isNil) { paperBlock.indent_(0) };
		if (paperBlock.systemSystemSpacing.isNil) { paperBlock.systemSystemSpacing_(0, 0, 10, 0) };
		if (paperBlock.scoreSystemSpacing.isNil) { paperBlock.scoreSystemSpacing_(10, 10, 10, 0) };
	}
	write { |argPath, openPDF=true|
		var paper, orientation;

		path = argPath ?? { String.nextDateStampedPathname(LP_Config.dir, "ly") };
		# paper, orientation = defaultPaperSize.collect { |each| each.asString };
		this.initDefaults;

		if (LP_Config.binExists.not) { ^error("Lilypond binary not found at" + LP_Config.bin) };

		File.use(path, "w", { |file|
			file.write("%" + Date.getDate.format("%Y-%m-%d %H:%M")).write("\n\n"); // date stamp
 			file.write("\\version" + LP_Config.version.asCompileString).write("\n");
			file.write("\\language" + LP_Config.language.asCompileString).write("\n\n");

			//!!! TODO: INCLUDES
			//file.write("\\include \"microtonal.ily\"").write("\n\n");

			file.write("#(set-default-paper-size" + paper.quote + "'" ++ orientation ++ ")").write("\n");
			file.write("#(set-global-staff-size" + globalStaffSize.asString ++ ")").write("\n\n");

			file.write(headerBlock.lpStr).write("\n\n");
			file.write(layoutBlock.lpStr).write("\n\n");
			file.write(paperBlock.lpStr).write("\n");
			if (music.isArray) { music.do { |each| file.write(each.lpStr) } } { file.write(music.lpStr) };
		});

		if (openPDF) { this.open };
	}
	open {
		var infile, outfile;
		infile = path.shellQuote;
		outfile = path.splitext[0].shellQuote;
		unixCmd(LP_Config.bin + "-o" + outfile + infile, action: { |result|
			if (result == 0) { unixCmd("open" + outfile ++ ".pdf") };
		});
	}
}


