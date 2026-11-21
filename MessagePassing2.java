import java.util.concurrent.CyclicBarrier;

/**
 * Comme MessagePassing, mais number et ready sont sur deux lignes de cache
 * différentes (deux objets alloués loin l'un de l'autre) : l'écrivain doit
 * obtenir deux lignes, et l'une peut arriver avant l'autre (diapos 2/7).
 *   javac MessagePassing2.java && java -Xint MessagePassing2
 */
public class MessagePassing2 {

  static class Nombre  { int v; }
  static class Drapeau { boolean v; }                    // essayer : volatile boolean v;

  static final int LOT = 10_000, LOTS = 2_000;           // 20 millions d'essais

  public static void main(String[] args) throws Exception {
    Nombre[]  number = new Nombre[LOT];
    Drapeau[] ready  = new Drapeau[LOT];
    int[] res = new int[LOT];                            // (ready ? 2 : 0) + (number == 42 ? 1 : 0)
    CyclicBarrier depart = new CyclicBarrier(3), fin = new CyclicBarrier(3);

    Thread ecrivain = new Thread(() -> {
      try {
        for (int k = 0; k < LOTS; k++) {
          depart.await();
          for (int i = 0; i < LOT; i++) { number[i].v = 42; ready[i].v = true; }
          fin.await();
        }
      } catch (Exception e) { throw new RuntimeException(e); }
    });
    Thread lecteur = new Thread(() -> {
      try {
        for (int k = 0; k < LOTS; k++) {
          depart.await();
          for (int i = LOT - 1; i >= 0; i--) {
            boolean r = ready[i].v;
            int n = number[i].v;
            res[i] = (r ? 2 : 0) + (n == 42 ? 1 : 0);
          }
          fin.await();
        }
      } catch (Exception e) { throw new RuntimeException(e); }
    });
    ecrivain.start();
    lecteur.start();

    long[] compte = new long[4];
    for (int k = 0; k < LOTS; k++) {
      // deux boucles séparées : les objets Nombre d'un côté, les Drapeau de l'autre,
      // donc number[i] et ready[i] sont à environ 160 Ko l'un de l'autre
      for (int i = 0; i < LOT; i++) number[i] = new Nombre();
      for (int i = 0; i < LOT; i++) ready[i]  = new Drapeau();
      depart.await();
      fin.await();
      for (int i = 0; i < LOT; i++) compte[res[i]]++;
    }

    System.out.printf("%s, %s%n", System.getProperty("os.arch"),
        System.getProperty("java.vm.info").contains("interpreted") ? "sans JIT (-Xint)" : "avec JIT");
    System.out.printf("ready = false, number =  0 : %,12d   lecteur en avance%n", compte[0]);
    System.out.printf("ready = false, number = 42 : %,12d   number écrit, ready pas encore%n", compte[1]);
    System.out.printf("ready = true,  number = 42 : %,12d   lecteur en retard%n", compte[3]);
    System.out.printf("ready = true,  number =  0 : %,12d   <-- ordre perdu%n", compte[2]);
  }
}
