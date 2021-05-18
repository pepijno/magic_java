package pepijno;

import jdk.jfr.Unsigned;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class Program {
    public enum Dir {
        NORTH,
        SOUTH,
        EAST,
        WEST,
        NORTH_EAST,
        NORTH_WEST,
        SOUTH_EAST,
        SOUTH_WEST
    }

    public static int nextIndex(Dir direction, int index) {
        int row = row(index);
        int col = col(index);

        switch (direction) {
            case NORTH: ++row; break;
            case SOUTH: --row; break;
            case EAST: ++col; break;
            case WEST: --col; break;
            case NORTH_EAST: ++row; ++col; break;
            case NORTH_WEST: ++row; --col; break;
            case SOUTH_EAST: --row; ++col; break;
            case SOUTH_WEST: --row; --col; break;
        }

        return (row > 7 || col > 7 || row < 0 || col < 0) ? -1 : index(row, col);
    }

    public static int edgeDistance(Dir direction, int index) {
        int row = row(index);
        int col = col(index);

        Function<Integer, Integer> inv = x -> 7 - x;

        int d = -1;

        switch (direction) {
            case NORTH: d = inv.apply(row); break;
            case SOUTH: d = row; break;
            case EAST: d = inv.apply(col); break;
            case WEST: d = col; break;
            case NORTH_EAST: d = Math.min(inv.apply(row), inv.apply(col)); break;
            case NORTH_WEST: d = Math.min(inv.apply(row), col); break;
            case SOUTH_EAST: d = Math.min(row, inv.apply(col)); break;
            case SOUTH_WEST: d = Math.min(row, col); break;
        }
        assert d >= 0 && d <= 7;
        return d;
    }

    public static long maskBits(Dir direction, int index) {
        long bitboard = 0L;
        int nextIndex = index;
        while ((nextIndex = nextIndex(direction, nextIndex)) >= 0 && nextIndex(direction, nextIndex) >= 0) {
            bitboard |= (1L << nextIndex);
        }
        return bitboard;
    }

    public static void generateOccupancy(List<Long> bbv, Dir direction, int index) {
        int numSquares = edgeDistance(direction, index) - 1;
        if (numSquares <= 0) {
            return;
        }

        int numOccupancies = (1 << numSquares);

        for (int occupancy = 0; occupancy < numOccupancies; ++occupancy) {
            long bitboard = 0L;
            int nextIndex = index;
            for (int bitMask = 1; bitMask <= occupancy; bitMask <<= 1) {
                nextIndex = nextIndex(direction, nextIndex);
                assert nextIndex != -1;
                bitboard |= (((long)(occupancy & bitMask)) << nextIndex);
            }
            bbv.add(bitboard);
        }
    }

    public static long generateAttack(Dir direction, int index, long occupancy) {
        long attackBB = 0L;
        for (int i = index; (i = nextIndex(direction, i)) != -1;) {
            attackBB |= (1L << i);
            if ((occupancy & (1L << i)) != 0) {
                break;
            }
        }
        return attackBB;
    }

    public static long random() {
        return new Random().nextLong();
    }

    public static long zeroBitsBiasedRandom() {
        return random() & random() & random();
    }

    public static long generateMagic(List<Dir> directions, int index, int shiftBits, List<Long> attackTable) {
        Occupancies combiner = new Occupancies(index);
        for (Dir direction : directions) {
            combiner.combine(direction);
        }
        List<Long> occupancies = combiner.getOccupancies();

        List<Long> attacks = new ArrayList<>();
        for (Long occupancy : occupancies) {
            long attack = 0L;
            for (Dir direction : directions) {
                attack |= generateAttack(direction, index, occupancy);
            }
            attacks.add(attack);
        }

        long kInvalidAttack = ~0L;

        while (true) {
            List<Long> table = new ArrayList<>();
            for (int i = 0; i < (1 << shiftBits); ++i) {
                table.add(kInvalidAttack);
            }
            long candidateMagic = zeroBitsBiasedRandom();
            boolean collision = false;
            for (int k = 0; k < occupancies.size(); ++k) {
                long occupancy = occupancies.get(k);
                long attack = attacks.get(k);
                int offset = (int)((occupancy * candidateMagic) >> (64 - shiftBits));
                if (table.get(offset) == kInvalidAttack || table.get(offset) == attack) {
                    table.add(offset, attack);
                } else {
                    collision = true;
                    break;
                }
            }
            if (!collision) {
                attackTable.clear();
                attackTable.addAll(table);
                return candidateMagic;
            }
        }
    }

    public static void main(String[] args) {

    }

    public static int index(int row, int col) {
        return row * 8 + col;
    }

    public static int row(int index) {
        return index / 8;
    }

    public static int col(int index) {
        return index % 8;
    }

    public static class Occupancies {
        private int index;
        private List<Long> occupancies;

        public Occupancies(int index) {
            this.index = index;
            this.occupancies = new ArrayList<>();
        }

        public List<Long> getOccupancies() {
            return this.occupancies;
        }

        public void combine(Dir direction) {
            List<Long> bbv = new ArrayList<>();
            generateOccupancy(bbv, direction, index);
            if (bbv.isEmpty()) {
                return;
            }
            if (occupancies.isEmpty()) {
                occupancies = bbv;
                return;
            }
            List<Long> tmp = new ArrayList<>();
            for (Long bb : bbv) {
                for (Long occupancy : occupancies) {
                    tmp.add(bb | occupancy);
                }
            }
            occupancies = tmp;
        }
    }
}
