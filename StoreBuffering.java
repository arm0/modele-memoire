import java.util.concurrent.CyclicBarrier;

/**
 * Motif de Dekker : chaque thread écrit sa variable, puis lit celle de l'autre.
 * Thread A : x = 1; r1 = y; Thread B : y = 1; r2 = x;
 * Peut-on obtenir r1 == 0 et r2 == 0 ?
 */
public class StoreBuffering {

  static class Etat {
    int x, y;
  } // essayer : volatile int x, y;

  static final int LOT = 10_000, LOTS = 2_000; // 20 millions d'essais

  public static void main(String[] args) throws Exception {
    Etat[] t = new Etat[LOT];
    int[] r1 = new int[LOT], r2 = new int[LOT];
    CyclicBarrier depart = new CyclicBarrier(3), fin = new CyclicBarrier(3);

    Thread a = new Thread(() -> {
      try {
        for (int k = 0; k < LOTS; k++) {
          depart.await();
          for (int i = 0; i < LOT; i++) {
            t[i].x = 1;
            r1[i] = t[i].y;
          }
          fin.await();
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    Thread b = new Thread(() -> {
      try {
        for (int k = 0; k < LOTS; k++) {
          depart.await();
          for (int i = 0; i < LOT; i++) {
            t[i].y = 1;
            r2[i] = t[i].x;
          }
          fin.await();
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    a.start();
    b.start();

    long zero = 0;
    for (int k = 0; k < LOTS; k++) {
      for (int i = 0; i < LOT; i++)
        t[i] = new Etat();
      depart.await();
      fin.await();
      for (int i = 0; i < LOT; i++)
        if (r1[i] == 0 && r2[i] == 0)
          zero++;
    }
    System.out.printf("r1 = 0 et r2 = 0 : %,d fois sur %,d\n", zero, (long) LOT * LOTS);
  }
}
