import java.util.concurrent.CyclicBarrier;

/**
 * Le lecteur peut-il voir ready == true et number == 0 ?
 *   javac MessagePassing.java && java MessagePassing
 *   java -Xint MessagePassing      sans JIT (interpréteur seul)
 *   puis déclarer ready volatile et relancer.
 */
public class MessagePassing {

  static class Etat { int number; boolean ready; }       // essayer : volatile boolean ready;

  static final int LOT = 10_000, LOTS = 2_000;           // 20 millions d'essais

  public static void main(String[] args) throws Exception {
    Etat[] t = new Etat[LOT];
    int[] res = new int[LOT];                            // (ready ? 2 : 0) + (number == 42 ? 1 : 0)
    CyclicBarrier depart = new CyclicBarrier(3), fin = new CyclicBarrier(3);

    // l'écrivain parcourt le lot dans l'ordre : number puis ready
    Thread ecrivain = new Thread(() -> {
      try {
        for (int k = 0; k < LOTS; k++) {
          depart.await();
          for (int i = 0; i < LOT; i++) { t[i].number = 42; t[i].ready = true; }
          fin.await();
        }
      } catch (Exception e) { throw new RuntimeException(e); }
    });
    // le lecteur le parcourt en sens inverse : il croise l'écrivain une fois par lot
    Thread lecteur = new Thread(() -> {
      try {
        for (int k = 0; k < LOTS; k++) {
          depart.await();
          for (int i = LOT - 1; i >= 0; i--) {
            boolean r = t[i].ready;
            int n = t[i].number;
            res[i] = (r ? 2 : 0) + (n == 42 ? 1 : 0);
          }
          fin.await();
        }
      } catch (Exception e) { throw new RuntimeException(e); }
    });
    ecrivain.start();
    lecteur.start();

    // main prépare chaque lot (objets neufs), donne le départ et compte
    long[] compte = new long[4];
    for (int k = 0; k < LOTS; k++) {
      for (int i = 0; i < LOT; i++) t[i] = new Etat();
      depart.await();                                    // les deux threads partent ensemble
      fin.await();
      for (int i = 0; i < LOT; i++) compte[res[i]]++;
    }

    System.out.printf("ready = false, number =  0 : %,12d   lecteur en avance%n", compte[0]);
    System.out.printf("ready = false, number = 42 : %,12d   number écrit, ready pas encore%n", compte[1]);
    System.out.printf("ready = true,  number = 42 : %,12d   lecteur en retard%n", compte[3]);
    System.out.printf("ready = true,  number =  0 : %,12d   <-- ordre perdu%n", compte[2]);
  }
}
