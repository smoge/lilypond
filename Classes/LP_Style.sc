LP_Style {
	var <context;
	*new { |context|
		^super.new.init(context);
	}
	init { |argScore|
		context = argScore;
		this.set;
	}
}

NoteHeadsOnly : LP_Style {
	set {
		var lpObj;
		lpObj = context.lpObj;
		context.override(lpObj ++ ".BarLine.stencil", "##f");
		context.override(lpObj ++ ".TimeSignature.stencil", "##f");
		context.override(lpObj ++ ".Stem.stencil", "##f");
		context.override(lpObj ++ ".Tie.stencil", "##f");
		context.override(lpObj ++ ".Beam.stencil", "##f");
		context.override(lpObj ++ ".Flag.stencil", "##f");
		context.override(lpObj ++ ".Dots.stencil", "##f");
		context.override(lpObj ++ ".NoteHead.duration-log", "#4");
		context.override(lpObj ++ ".Rest.transparent", "##t");
		// only show first event in ties
		switch(context.class,
			LP_Score, { context.staves.do { |staff| this.hideTiedNotes(staff) } },
			LP_Staff, { this.hideTiedNotes(context) }
		);
	}
	hideTiedNotes { |staff|
		staff.selectBy(LP_PitchEvent).components.do { |sel|
			if (sel.isKindOf(LP_TieSelection)) {
				sel[1..].override('NoteHead.transparent', "##t").override('NoteHead.no-ledgers', "##t");
			};
		};
	}
}