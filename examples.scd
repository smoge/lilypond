/* ---------------------------------------------------------------------------------------------------------------------
• Brian Ferneyhough: generation of rhythmic cells for Unsichtbare Farben
- example adapted from Abjad: http://abjad.mbrsi.org/literature_examples/ferneyhough.html
---------------------------------------------------------------------------------------------------------------------- */
(
var cells, staves, score;
cells = (1..11).collect { |i| (1..6).collect { |j| [1, LP_Tuplet(i, 1!j)] } };
staves = cells.collect { |ratios| LP_RhythmicStaff(ratios.collect { |ratio| LP_Measure([1, 4], ratio) }) };
score = LP_Score(staves);
score.proportionalNotationDuration_(LP_Duration(1, 56));
score.showTimeSignatures_(false);
score.showBarLines_(false);
LP_File(score).write;
)
/* ---------------------------------------------------------------------------------------------------------------------
• Steve Reich: Clapping Music

!! TODO: Grob: priority, padding, etc. having no effect when Grob is attached to LP_Measure
---------------------------------------------------------------------------------------------------------------------- */
(
var pattern, rotations, staff1, staff2, score, file;

pattern = [1,1,1,-1,1,1,-1,1,-1,1,1,-1];
rotations = (0..-12).collect { |i| pattern.rotate(i) };
staff1 = LP_RhythmicStaff(LP_Measure([3, 2], 1!12) ! rotations.size);
staff2 = staff1.copy;
staff1.selectBy(LP_Leaf).mask((pattern ! rotations.size).flat);
staff2.selectBy(LP_Leaf).mask(rotations.flat);
score = LP_Score([staff1, staff2]);
file = LP_File(score);

staff1.measures[0].attach(LP_MetronomeMark(LP_Duration(1, 4), "160-184").padding_(6)); // attach tempo indication
staff1.measures.do { |measure, i| measure.attach(LP_RehearsalMark((i+1).asString).priority_(1)) };

staff2.stemDirection_(\down);

staff1.measures[0].attach(LP_BarLine(\startRepeat)); // attach opening repeat barline
staff1.measures[1..].do { |measure| measure.attach(LP_BarLine(\dualRepeat)) }; // attach repeat barlines
staff1.measures.last.attachToTail(LP_BarLine(\endRepeat)); // attach closing repeat barline

score.proportionalNotationDuration_(LP_Duration(2, 15));
score.showBarNumbers_(false);
score.showTimeSignatures_(false);

file.defaultPaperSize_('a4', 'portrait');
file.globalStaffSize_(20);
file.paperBlock.systemSystemSpacing_(30, 30, 10, 0);
file.paperBlock.raggedRight_(true);
file.headerBlock.title_("Clapping Music");
file.headerBlock.composer_("Steve Reich");
file.write;
)
/* ---------------------------------------------------------------------------------------------------------------------
• pass multiple scores to a LP_File
---------------------------------------------------------------------------------------------------------------------- */
(
var timeSigs, scores, file;

timeSigs = [[3, 4], [7, 8], [4, 4], [9, 8], [5, 4]];

scores = timeSigs.collect { |timeSig, i|
	var markup, measures, score;

	markup = LP_Markup("Score" + (i+1).asString, \above, \right, -3.2);
	markup.font_(Font("times new roman", 10));
	markup.color_(Color.grey);
	markup.box_(true).padding_(0.2);

	measures = { LP_Measure(timeSig, [1, -1, 1, 4], [61, 62, 63]) } ! 4;
	score = LP_Score([LP_Staff(measures)]);
	score[0].attach(markup);
	score.proportionalNotationDuration_(LP_Duration(1, 12));
	score.showTupletRatios_(true);
	score;
};

LP_File(scores).write;
)