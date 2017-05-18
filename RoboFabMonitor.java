import es.upm.babel.cclib.Monitor;

public class RoboFabMonitor implements RoboFab{
	
	/*
	 * 
	 */

	/*
	 * Dominio
	 */
	private int p;// peso que tiene actualmente la caja
	private int[] pends = new int[Robots.NUM_ROBOTS];//Array de pesos pendientes que quedan por poner en la caja
	
	/*
	 * Creacion de monitores
	 */
	private Monitor mutex;
    private Monitor.Cond espRobots[];//Espera Robots
    private Monitor.Cond espCinta;//Espera cinta

    
    /*
	 * Constructor
	 * Inicial :
	 * 			p = 0
	 * 			pends = 
	 */
	public RoboFabMonitor(){
		p = 0;
		mutex = new Monitor();
		espCinta = mutex.newCond();
		espRobots = new Monitor.Cond[Robots.NUM_ROBOTS];
		for (int i = 0; i < pends.length; i++) {
			pends[i]=0;
			espRobots[i]= mutex.newCond();
		}
		
	}
	
	/*
	 * Invariante: self.peso <= MAX_P_CONTENEDOR
	 */
	private boolean invariante(){
		return this.p <= Cinta.MAX_P_CONTENEDOR;
    }
	
	/*
	 * Verifica la invariante
	 */
	private void test_invariante(){
	   		String msg = "La invariante no se cumple\nEstado incorrecto: ( " + this.p + " > " + Cinta.MAX_P_CONTENEDOR + ")";
	        if(!invariante())
				throw new RuntimeException(new Exception(msg));
	   }

	@Override
	/*
	 *  i : idRobot (0-4)
	 *  p : carga (MIN_P_PIEZA >= carga =< MAX_P_PIEZA)
	 *  Salida : Vacia
	 *  Información: Guarda el peso de la carga (p) de un robot (i)
	 *  	para identificar que tiene una carga pendiente de soltar.
	 */
	public void notificarPeso(int i, int p) {	
		mutex.enter();
		pends[i] = p;//actualiza el peso de cada robot 
		mutex.leave();
	}

	@Override
	/*
	 * i: idRobot (0-4)
	 * Salida: Vacia
	 * Información:
	 */
	public void permisoSoltar(int i) {
		mutex.enter();
		
		if(this.p + pends[i] > Cinta.MAX_P_CONTENEDOR){
			espRobots[i].await();
		}
		
		p += pends[i];//El robot pone la carga en la caja y suma el peso que hay en la caja
		pends[i]=0;//El robot ha soltado el peso en la caja 
		
		desbloquear();
		
		test_invariante();
		
		mutex.leave();
	}

	@Override
	/* 
	 * Salida: Vacia
	 * Información: Si hay robots soltando piezas espera.
	 */
	public void solicitarAvance() {
		mutex.enter();
		
		if(true){
			espCinta.await();
		}
		
		test_invariante();
		
		mutex.leave();

	}

	@Override
	/* 
	 * Salida: Vacia
	 * Información: Vuelve p a su estado inicial indicando
	 * 		que hay un nuevo contenedor en la cinta.
	 */
	public void contenedorNuevo() {
		mutex.enter();
		this.p = 0;
		desbloquear();
		test_invariante();
		mutex.leave();
	}

	/*
	 * 
	 */
	private void desbloquear(){
		boolean señal = false;
		if(true ){
		}
		if(espCinta.waiting() > 0 && !señal){
			espCinta.signal();
			señal = true;
		}
	}

}
