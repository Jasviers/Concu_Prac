import org.jcsp.lang.Alternative;
import org.jcsp.lang.AltingChannelInput;
import org.jcsp.lang.Any2OneChannel;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.ChannelOutput;
import org.jcsp.lang.Guard;
import org.jcsp.lang.One2OneChannel;


class PetNotificar {
	private int robotId;
	private int peso;

	public PetNotificar(int robotId, int peso) {
		this.robotId = robotId;
		this.peso = peso;
	}

	public int getRobotId() {
		return robotId;
	}

	public int getPeso() {
		return peso;
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
		int peso;
		int[] pends;

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
		for(int i =0; i<Robots.NUM_ROBOTS;i++){
			guards[i]=chSoltar[i].in();
		}
		final int NOTIFICAR = Robots.NUM_ROBOTS;
		final int AVANZAR   = Robots.NUM_ROBOTS+1;
		final int NUEVO     = Robots.NUM_ROBOTS+2;
		// 
		guards[NOTIFICAR] = chNotificar.in();
		guards[AVANZAR]   = chAvanzar.in();
		guards[NUEVO]     = chNuevo.in();

		// array de booleanos para sincronización por condición
		boolean enabled[] = new boolean[Robots.NUM_ROBOTS+3];
		// inicializamos las condiciones de activación de los canales
		boolean aux = true;
		for(int i =0; i<Robots.NUM_ROBOTS;i++){
			enabled[i]=peso+pends[i] <= Cinta.MAX_P_CONTENEDOR;
			aux = aux && !enabled[i];
		}
		enabled[NOTIFICAR]= true;
		enabled[AVANZAR]= aux;
		enabled[NUEVO]= true;
		final Alternative services = new Alternative(guards);

		while (true) {
			// refrescamos el vector enabled:
			aux = true;
			for(int i =0; i<Robots.NUM_ROBOTS;i++){
				enabled[i]=peso+pends[i] <= Cinta.MAX_P_CONTENEDOR;
				aux = aux && !enabled[i];
			}
			enabled[NOTIFICAR]= true;
			enabled[AVANZAR]= aux;
			enabled[NUEVO]= true;

			// la SELECT:
			int i = services.fairSelect(enabled);
			if (i == NOTIFICAR) {
				ChannelOutput chResp = (ChannelOutput) chNotificar.in().read();
				//pends[]= ;
			} else if (i == AVANZAR) {
				ChannelOutput chResp = (ChannelOutput) chAvanzar.in().read();
			} else if (i == NUEVO) {
				ChannelOutput chResp = (ChannelOutput) chNuevo.in().read();
				peso = 0;
			} else{ // permisoSoltar
				ChannelOutput chResp = (ChannelOutput) chSoltar[i].in().read();
				peso = pends[i];
				pends[i]=0;
			} 
		}
	}	
}
