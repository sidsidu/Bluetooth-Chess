# Piece Assets 

This folder acts as a placeholder or reference structure as originally requested. 

The Android app loads the SVG pieces from `app/src/main/res/raw/` at runtime, ensuring they are bundled within the application via standard Android resource handling protocols. The naming convention for the SVG files in this directory must strictly match the following format in lowercase:

- `white_king.svg`
- `white_queen.svg`
- `white_rook.svg`
- `white_bishop.svg`
- `white_knight.svg`
- `white_pawn.svg`
- `black_king.svg`
- `black_queen.svg`
- `black_rook.svg`
- `black_bishop.svg`
- `black_knight.svg`
- `black_pawn.svg`

The custom SVG loader within `ChessBoardView.kt` automatically pulls matching files from compiling raw resources corresponding to these filenames using AndroidSVG.
