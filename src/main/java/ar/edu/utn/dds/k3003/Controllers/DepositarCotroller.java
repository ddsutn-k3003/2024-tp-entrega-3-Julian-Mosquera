package ar.edu.utn.dds.k3003.Controllers;

import ar.edu.utn.dds.k3003.app.Fachada;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

public class DepositarCotroller implements Handler {
    private  Fachada fachada;
    public DepositarCotroller(Fachada fachada) {
        this.fachada = fachada;
    }


    @Override
    public void handle(@NotNull Context context) throws Exception {
        String trasladoId = context.pathParam("trasladoId");
        Long id = Long.parseLong(trasladoId);
        fachada.trasladoDepositado(id);
        context.result("Vianda depositada correctamente");
    }
}
