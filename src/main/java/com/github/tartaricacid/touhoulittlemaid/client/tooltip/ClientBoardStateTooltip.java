package com.github.tartaricacid.touhoulittlemaid.client.tooltip;

import com.github.tartaricacid.touhoulittlemaid.api.game.gomoku.GomokuCodec;
import com.github.tartaricacid.touhoulittlemaid.api.game.gomoku.Point;
import com.github.tartaricacid.touhoulittlemaid.inventory.tooltip.BoardStateTooltip;
import com.github.tartaricacid.touhoulittlemaid.util.CChessUtil;
import com.github.tartaricacid.touhoulittlemaid.util.GuiTools;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.github.tartaricacid.touhoulittlemaid.util.WChessUtil;
import net.minecraft.util.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

import static com.github.tartaricacid.touhoulittlemaid.inventory.tooltip.BoardStateTooltip.CHESS;
import static com.github.tartaricacid.touhoulittlemaid.inventory.tooltip.BoardStateTooltip.GOMOKU;
import static com.github.tartaricacid.touhoulittlemaid.inventory.tooltip.BoardStateTooltip.XIANGQI;

/**
 * 残局道具按住 Shift 时的棋盘预览（原实现见 1.21.1 同名类）。
 * 26.3 的接口换成了 extractImage / GuiGraphicsExtractor，另外棋盘按 1:1 画（原版用 pose 缩放到 0.5）。
 */
public class ClientBoardStateTooltip implements ClientTooltipComponent {
    private static final Identifier GOMOKU_BG = IdentifierUtil.modLoc("textures/gui/gomoku.png");
    private static final Identifier XIANGQI_BG = IdentifierUtil.modLoc("textures/gui/xiangqi.png");
    private static final Identifier CHESS_BG = IdentifierUtil.modLoc("textures/gui/chess.png");

    private static final BiFunction<String, String, Object> CACHE = Util.memoize(ClientBoardStateTooltip::getBoardGameData);

    private final String type;
    private final Object boardGameData;

    public ClientBoardStateTooltip(BoardStateTooltip tooltip) {
        this.boardGameData = CACHE.apply(tooltip.type(), tooltip.stateData());
        this.type = tooltip.type();
    }

    @Nullable
    private static Object getBoardGameData(String type, String stateData) {
        try {
            switch (type) {
                case GOMOKU -> {
                    return GomokuCodec.decode(stateData).board();
                }
                case XIANGQI -> {
                    com.github.tartaricacid.touhoulittlemaid.api.game.xqwlight.Position position = new com.github.tartaricacid.touhoulittlemaid.api.game.xqwlight.Position();
                    position.fromFen(stateData);
                    return position.squares;
                }
                case CHESS -> {
                    com.github.tartaricacid.touhoulittlemaid.api.game.chess.Position position = new com.github.tartaricacid.touhoulittlemaid.api.game.chess.Position();
                    position.fromFen(stateData);
                    return position.squares;
                }
                default -> {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int getHeight(Font font) {
        if (this.boardGameData == null) {
            return 0;
        }
        return switch (this.type) {
            case GOMOKU -> 151;
            case XIANGQI -> 126;
            case CHESS -> 204;
            default -> 0;
        };
    }

    @Override
    public int getWidth(Font font) {
        if (this.boardGameData == null) {
            return 0;
        }
        return switch (this.type) {
            case GOMOKU -> 151;
            case XIANGQI -> 128;
            case CHESS -> 204;
            default -> 0;
        };
    }

    @Override
    public void extractImage(Font font, int pX, int pY, int w, int h, GuiGraphicsExtractor graphics) {
        if (this.boardGameData == null) {
            return;
        }
        if (GOMOKU.equals(this.type) && this.boardGameData instanceof byte[][] data) {
            this.renderGomoku(pX, pY, graphics, data);
            return;
        }
        if (XIANGQI.equals(this.type) && this.boardGameData instanceof byte[] data) {
            this.renderXiangqi(pX, pY, graphics, data);
            return;
        }
        if (CHESS.equals(this.type) && this.boardGameData instanceof byte[] chessData) {
            this.renderChess(pX, pY, graphics, chessData);
        }
    }

    private void renderGomoku(int pX, int pY, GuiGraphicsExtractor graphics, byte[][] data) {
        GuiTools.guiBlit(graphics, GOMOKU_BG, pX, pY, 0, 0, 151, 151);
        for (int y = 0; y <= 14; y++) {
            for (int x = 0; x <= 14; x++) {
                int piecesIndex = data[x][y];
                int v;
                if (piecesIndex == Point.BLACK) {
                    v = 151;
                } else if (piecesIndex == Point.WHITE) {
                    v = 160;
                } else {
                    continue;
                }
                GuiTools.guiBlit(graphics, GOMOKU_BG, pX + 1 + x * 10, pY + 1 + y * 10, 0, v, 9, 9);
            }
        }
    }

    private void renderXiangqi(int pX, int pY, GuiGraphicsExtractor graphics, byte[] data) {
        GuiTools.guiBlit(graphics, XIANGQI_BG, pX, pY, 0, 0, 128, 126);
        for (int y = 3; y <= 12; y++) {
            for (int x = 3; x <= 11; x++) {
                byte piecesIndex = CChessUtil.piecesIndex(x, y, data);
                int v;
                int u;
                if (CChessUtil.isRed(piecesIndex)) {
                    v = 126;
                    u = (piecesIndex - 8) * 11;
                } else if (CChessUtil.isBlack(piecesIndex)) {
                    v = 137;
                    u = (piecesIndex - 16) * 11;
                } else {
                    continue;
                }
                GuiTools.guiBlit(graphics, XIANGQI_BG, pX - 1 + (x - 3) * 13, pY - 1 + (y - 3) * 13, u, v, 11, 11);
            }
        }
    }

    private void renderChess(int pX, int pY, GuiGraphicsExtractor graphics, byte[] data) {
        GuiTools.guiBlit(graphics, CHESS_BG, pX, pY, 0, 0, 204, 204);
        for (int y = 0; y <= 7; y++) {
            for (int x = 4; x <= 11; x++) {
                byte piecesIndex = WChessUtil.piecesIndex(x, y, data);
                int v;
                int u;
                if (WChessUtil.isWhite(piecesIndex)) {
                    v = 204;
                    u = (piecesIndex - 8) * 24;
                } else if (WChessUtil.isBlack(piecesIndex)) {
                    v = 228;
                    u = (piecesIndex - 16) * 24;
                } else {
                    continue;
                }
                GuiTools.guiBlit(graphics, CHESS_BG, pX + 6 + (x - 4) * 24, pY + 6 + y * 24, u, v, 24, 24);
            }
        }
    }
}
