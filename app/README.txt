this is a sudoku solver app that utilizes OCR to read user boards

v0.1 09/02/22
- only accepts gallery images for user provided boards
- does basic image preprocessing with binarization -> skew correction capable of handling up to 5 degrees of tilt
- uses user provided cropping for edge definition of provided board
- uses android ML Kit for OCR text processing
- solver is capable of handling beginner to medium difficulty boards (hidden/naked singles)

TODO:
- [] provide program structure infographic
- [] revisit program structure and design with knowledge about best practices
- [] allow taking live photos with camera as option to provide game board
- [] clean up image preprocessing code, and optimize skew correction algorithm by layering multiple searches
- [] use existing ML solution or invent wheel to provide shape recognition for board edges instead having users crop
- [] refine OCR text processing by adding spot query capabilities to provide more robust reads on user image
-  improve solver to handle more advanced inference techniques, i.e.
    - [] hidden/naked doubles
    - [] hidden/naked triplets
    - [] xWing
    - [] coloring
    - [] something something boundaries...xyz cant remember