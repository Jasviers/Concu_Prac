import java.awt.Robot;

import org.jcsp.lang.Alternative;
import org.jcsp.lang.AltingChannelInput;
import org.jcsp.lang.Any2OneChannel;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.ChannelOutput;
import org.jcsp.lang.Guard;
import org.jcsp.lang.One2OneChannel;
    

class PetNotificar {
    int robotId;
    int peso;
    
    public PetNotificar(int robotId, int peso) {
	this.robotId = robotId;
	this.peso = peso;
    }
}

// RoboFabCSP: Solución con replicación de canales
// Completad las líneas marcadas "TO DO"

public class RoboFabCSP implements RoboFab, CSProcess {


    // Un canal para notificarPeso
    Any2OneChannel chNotificar;
    // NUM_ROBOTS canales para permisoSoltar
    Any2OneChannel chSoltar[];
    // Un canal para solicitarAvance
    Any2OneChannel chAvanzar;
    // Un canal para contenedorNuevo
    Any2OneChannel chNuevo;
    
    public RoboFabCSP() {

	// Creamos los canales
    	chNotificar = Channel.any2one();
	chSoltar = new Any2OneChannel[Robots.NUM_ROBOTS];
	for (int i=0; i<Robots.NUM_ROBOTS; i++) chSoltar[i] = Channel.any2one();
	chAvanzar = Channel.any2one();
	chNuevo = Channel.any2one();
    }

    public void permisoSoltar(int robotId) {
	chSoltar[robotId].out().write(null);
    }

    public void notificarPeso(int robotId, int peso) {
	PetNotificar pet = new PetNotificar(robotId,peso);
	chNotificar.out().write(pet);
    }

    public void solicitarAvance() {
	chAvanzar.out().write(null);
    }

    public void contenedorNuevo() {
	chNuevo.out().write(null);
    }

    public void run() {
	// declaramos estado del recurso: peso, pendientes...
	int peso;//peso que tiene actualmente la caja
	int[] pends;//Array de pesos pendientes que quedan por poner en la caja

	// Inicializamos el estado del recurso
	peso = 0;
	pends = new int[Robots.NUM_ROBOTS];
	for(int i = 0; i<pends.length; i++){
		pends[i]=0;
		}

	// Estructuras para recepción alternativa condicional
	final AltingChannelInput[] guards = new AltingChannelInput[Robots.NUM_ROBOTS+3];
	// reservamos NUM_ROBOTS entradas para permisoSoltar y una entrada cada una de
	// notificarPeso, solicitarAvance y contenedorNuevo
	for(int i =3; i<Robots.NUM_ROBOTS;i++){
		guards[i] = chSoltar[i].in();
	}
	final int NOTIFICAR = 0;
	final int AVANZAR   = 1;
	final int NUEVO     = 2;
	// 
	guards[NOTIFICAR] = chNotificar.in();
	guards[AVANZAR]   = chAvanzar.in();
	guards[NUEVO]     = chNuevo.in();

	// array de booleanos para sincronización por condición
	boolean enabled[] = new boolean[Robots.NUM_ROBOTS+3];
	// inicializamos las condiciones de activación de los canales
	
	enabled[NOTIFICAR]= true;
	enabled[NUEVO]= true;
	final Alternative services = new Alternative(guards);

	while (true) {
	    // refrescamos el vector enabled:
		boolean aux = true; //Creamos un booleano 
		for(int i =0; i<Robots.NUM_ROBOTS;i++){
			enabled[i]= peso + pends[i] <= Cinta.MAX_P_CONTENEDOR;// Comprobamos el CPRE y ponemos a true o false dependiendo de si se cumple o no 
		}
		
		for(int i =0; i<Robots.NUM_ROBOTS && aux;i++){
		if(peso+pends[i] <= Cinta.MAX_P_CONTENEDOR){ aux = false; }
		}

		enabled[AVANZAR]= aux;
		PetNotificar peticion= null;
	    // la SELECT:
	    int i = services.fairSelect(enabled);
	    if (i == NOTIFICAR) {
	    peticion = (PetNotificar)chNotificar.in().read();
		pends[peticion.robotId] = peticion.peso;
	    } else if (i == AVANZAR) {
		chAvanzar.in().read();
	    } else if (i == NUEVO) {
		chNuevo.in().read();
		peso=0;
	    } else{ // permisoSoltar
		chSoltar[i].in().read();
		peso= peso + pends[i];
		pends[i]=0;
	    } 
	}
    }	
}
