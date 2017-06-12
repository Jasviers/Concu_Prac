import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import es.upm.babel.cclib.Monitor;

public class RoboFabMonitor implements RoboFab{
	
	/*
	 * Dominio
	 * p = N
	 * pends = [N]
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
	 * 	p = 0
	 * 	pends = [0,0,...,0]
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
		return this.p <= Cinta.MAX_P_CONTENEDOR;// El peso debe ser menor que el maximo peso del contenedor
    }
	
	/*
	 * Verifica la invariante
	 */
	private void test_invariante(){
	   		String msg = "La invariante no se cumple: ( " + this.p + " > " + Cinta.MAX_P_CONTENEDOR + ")";
	        if(!invariante())
				throw new RuntimeException(new Exception(msg));
	   }

	@Override
	/*
	 *  i : idRobot (0-4)
	 *  p : carga (MIN_P_PIEZA >= carga =< MAX_P_PIEZA)
	 *  Salida : Vacia
	 *  Informacion: Guarda el peso de la carga (p) de un robot (i)
	 *  	para identificar que tiene una carga pendiente de soltar.
	 */
	public void notificarPeso(int i, int p) {	
		mutex.enter();
		pends[i] = p;//actualiza el peso de cada robot 
		desbloquear();//Desbloquea el hilo pertinente
		mutex.leave();
	}

	@Override
	/*
	 * i: idRobot (0-4)
	 * Salida: Vacia
	 * Informacion: Comprueba que puede soltar la carga, si puede
	 * la suelta en el contenedor, si no se bloquea.
	 */
	public void permisoSoltar(int i) {
		mutex.enter();
		test_invariante();//comprueba que no viola la invariante
		
		if(p + pends[i] > Cinta.MAX_P_CONTENEDOR){//Comprobacion cpre
			espRobots[i].await();//Pone el robot en espera
		}
		
		p += pends[i];//El robot pone la carga en la caja y suma el peso que hay en la caja
		pends[i]=0;//El robot ha soltado el peso en la caja 
		desbloquear();//Desbloquea el hilo pertinente
		mutex.leave();
	}

	@Override
	/* 
	 * Salida: Vacia
	 * Informacion: Si hay robots soltando piezas espera.
	 */
	public void solicitarAvance() {
		mutex.enter();		
		if (!solAv()){//comprobacion cpre
			espCinta.await();//Pone la cinta en espera
		}
		test_invariante();//comprueba que no viola la invariante
		desbloquear();//Desbloquea el hilo pertinente
		mutex.leave();

	}

	@Override
	/* 
	 * Salida: Vacia
	 * Informacion: Vuelve p a su estado inicial indicando
	 * 		que hay un nuevo contenedor en la cinta.
	 */
	public void contenedorNuevo() {
		mutex.enter();
		this.p = 0;//pone el peso a 0
		desbloquear();//Desbloquea el hilo pertinente
		test_invariante();//comprueba que no viola la invariante
		mutex.leave();
	}
	
	/*
	 * Metodos auxiliares
	 * solAv: Devuelve un valor booleano dependiente de la cpre de solicitarAvance.
	 * desbloquear: conprueba que hilo tiene que desbloquear y lo desbloquea.
	 */
	 private boolean solAv(){
		 boolean signal= true;
		 for (int i = 0; i < pends.length; i++) {	
			signal= signal && p+pends[i]>Cinta.MAX_P_CONTENEDOR;
				
		 }
		 return signal;
	 }

	private void desbloquear(){
        boolean senal = false;// booleano auxiliar de parada y limitante
        for (int i = 0; i < espRobots.length && !senal; i++) {
            if(espRobots[i].waiting()>0 && this.p + pends[i] <= Cinta.MAX_P_CONTENEDOR ){
                espRobots[i].signal();
                senal = true;
            } 
        }
        if(espCinta.waiting() > 0 && !senal&& solAv()){
            espCinta.signal();
        }
    }

}
