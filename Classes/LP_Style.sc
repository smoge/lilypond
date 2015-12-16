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
		//!!! context.override(lpObj ++ ".NoteHead.duration-log", "#4");

		context.showBarLines_(false);
		context.showTimeSignatures_(false);
		context.showStems_(false);
		context.showTies_(false);
		context.showBeams_(false);
		context.showFlags_(false);
		context.showDots_(false);
		context.showRests_(false);

		// only show first event in ties
		switch(context.class,
			LP_Score, { context.staves.do { |staff| this.hideTiedNotes(staff) } },
			LP_Staff, { this.hideTiedNotes(context) }
		);
	}
	hideTiedNotes { |staff|
		staff.selectBy(LP_PitchEvent).components.do { |sel|
			if (sel.isKindOf(LP_TieSelection)) {
				// sel[1..].override(\noteHeadTransparent, LP_Override("NoteHead.transparent = ##t", true));
				// sel[1..].override(\noteHeadNoLedgers, LP_Override("NoteHead.no-ledgers = ##t", true));
				sel[1..].do { |note|
					note.override(\noteHeadTransparent, LP_Override("NoteHead.transparent = ##t", true));
					note.override(\noteHeadNoLedgers, LP_Override("NoteHead.no-ledgers = ##t", true));
				};
			};
		};
	}
}
