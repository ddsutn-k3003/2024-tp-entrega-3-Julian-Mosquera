package ar.edu.utn.dds.k3003.app;

import ar.edu.utn.dds.k3003.facades.FachadaHeladeras;
import ar.edu.utn.dds.k3003.facades.FachadaLogistica;
import ar.edu.utn.dds.k3003.facades.FachadaViandas;
import ar.edu.utn.dds.k3003.facades.dtos.*;
import ar.edu.utn.dds.k3003.facades.exceptions.TrasladoNoAsignableException;
import ar.edu.utn.dds.k3003.model.Ruta;
import ar.edu.utn.dds.k3003.model.Traslado;
import ar.edu.utn.dds.k3003.repositorios.RepositorioRuta;
import ar.edu.utn.dds.k3003.repositorios.RepositorioTraslado;
import ar.edu.utn.dds.k3003.repositorios.RutaMapper;
import ar.edu.utn.dds.k3003.repositorios.TrasladoMapper;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

public class Fachada implements FachadaLogistica {
    private EntityManager entityManager;
    private final RepositorioRuta repositorioRuta;
    private final RepositorioTraslado repositorioTraslado;
    private final RutaMapper rutaMapper;
    private final TrasladoMapper trasladoMapper;
    private FachadaViandas fachadaViandas;
    private FachadaHeladeras fachadaHeladeras;

    public Fachada(EntityManager entityManager){
        this.entityManager = entityManager;
        this.repositorioRuta = new RepositorioRuta(entityManager);
        this.rutaMapper = new RutaMapper();
        this.repositorioTraslado = new RepositorioTraslado(entityManager);
        this.trasladoMapper = new TrasladoMapper();
    }
    public Fachada(){
        this.repositorioRuta = new RepositorioRuta(entityManager);
        this.rutaMapper = new RutaMapper();
        this.repositorioTraslado = new RepositorioTraslado(entityManager);
        this.trasladoMapper = new TrasladoMapper();
    }
@Override
public RutaDTO agregar(RutaDTO rutaDTO){
    Ruta ruta = new Ruta(rutaDTO.getColaboradorId(), rutaDTO.getHeladeraIdOrigen(), rutaDTO.getHeladeraIdDestino());
    entityManager.getTransaction().begin();
    ruta = this.repositorioRuta.guardar(ruta);
    entityManager.getTransaction().commit();
    return rutaMapper.mapear(ruta);
}

@Override
    public TrasladoDTO buscarXId(Long trasladoID) throws NoSuchElementException{
        entityManager.getTransaction().begin();
        TrasladoDTO traslado = trasladoMapper.mapear(repositorioTraslado.buscarXId(trasladoID));
        entityManager.getTransaction().commit();
        return  traslado;
}

    public  RutaDTO buscarRutaXId(Long rutaID) throws NoSuchElementException{
        entityManager.getTransaction().begin();
        RutaDTO ruta = rutaMapper.mapear(repositorioRuta.buscarXId(rutaID));
        entityManager.getTransaction().commit();
        return ruta;
    }


@Override
    public TrasladoDTO asignarTraslado(TrasladoDTO trasladoDTO) throws TrasladoNoAsignableException{
    entityManager.getTransaction().begin();
    ViandaDTO viandaDTO = fachadaViandas.buscarXQR(trasladoDTO.getQrVianda());
    Random random = new Random();
    List<Ruta> rutasPosibles = repositorioRuta.buscarXHeladera(trasladoDTO.getHeladeraOrigen(), trasladoDTO.getHeladeraDestino());
    if(rutasPosibles.isEmpty()){
        throw new TrasladoNoAsignableException("No se encontraron rutas posibles para el traslado");
    }
    Ruta ruta = rutasPosibles.get(random.nextInt(rutasPosibles.size()));
    Traslado traslado = new Traslado(viandaDTO.getCodigoQR(), EstadoTrasladoEnum.ASIGNADO, trasladoDTO.getFechaTraslado(), ruta);
    repositorioTraslado.guardar(traslado);
    entityManager.getTransaction().commit();
    return trasladoMapper.mapear(traslado);

    }

@Override
    public List<TrasladoDTO> trasladosDeColaborador(Long colaboradorId, Integer mes, Integer anio){
       entityManager.getTransaction().begin();
        List<Traslado> traslados = repositorioTraslado.buscarXColaborador(colaboradorId);
        List<Traslado> trasladosFiltrados = traslados.stream().filter(t -> t.getFechaTraslado().getMonthValue() == mes &&
                                                                        t.getFechaTraslado().getYear() == anio).toList();
        entityManager.getTransaction().commit();
        return trasladosFiltrados.stream().map(trasladoMapper :: mapear).toList();
}

@Override
    public void trasladoRetirado(Long trasladoId){
        entityManager.getTransaction().begin();
        Traslado traslado = repositorioTraslado.buscarXId(trasladoId);
        RetiroDTO retiro = new RetiroDTO(traslado.getQrVianda(), "321", traslado.getRuta().getHeladeraOrigen());
        fachadaHeladeras.retirar(retiro);
        fachadaViandas.modificarEstado(traslado.getQrVianda(), EstadoViandaEnum.EN_TRASLADO);
        repositorioTraslado.guardar(new Traslado(traslado.getQrVianda(), EstadoTrasladoEnum.EN_VIAJE, traslado.getFechaTraslado(), traslado.getRuta()));
        entityManager.getTransaction().commit();
}

@Override
    public void trasladoDepositado(Long trasladoId){
    entityManager.getTransaction().begin();
    TrasladoDTO traslado = trasladoMapper.mapear(repositorioTraslado.buscarXId(trasladoId));
    fachadaHeladeras.depositar(traslado.getHeladeraDestino(), traslado.getQrVianda());
    fachadaViandas.modificarHeladera(traslado.getQrVianda(),traslado.getHeladeraDestino());
    fachadaViandas.modificarEstado(traslado.getQrVianda(), EstadoViandaEnum.DEPOSITADA);

    repositorioTraslado.guardar(new Traslado(traslado.getQrVianda(), EstadoTrasladoEnum.ENTREGADO, traslado.getFechaTraslado(),
            new Ruta(traslado.getColaboradorId(), traslado.getHeladeraOrigen(), traslado.getHeladeraDestino())));
    entityManager.getTransaction().commit();
}

    public List<RutaDTO> rutas(){
        return repositorioRuta.todos().stream().map(rutaMapper::mapear).toList();
    }

    public List<TrasladoDTO> traslados(){
        return repositorioTraslado.todos().stream().map(trasladoMapper::mapear).toList();
    }

    public void modificarEstadoTraslado(Long trasladoId, EstadoTrasladoEnum nuevoEstado) throws NoSuchElementException{
        entityManager.getTransaction().begin();
        repositorioTraslado.modificarEstadoXID(trasladoId, nuevoEstado);
        entityManager.getTransaction().commit();
    }


@Override
public void setHeladerasProxy(FachadaHeladeras fachadaHeladeras){this.fachadaHeladeras = fachadaHeladeras;}
@Override
public void setViandasProxy(FachadaViandas fachadaViandas){this.fachadaViandas = fachadaViandas;}



    public RepositorioRuta getRepositorioRuta() {
        return repositorioRuta;
    }

    public RepositorioTraslado getRepositorioTraslado() {
        return repositorioTraslado;
    }

    public RutaMapper getRutaMapper() {
        return rutaMapper;
    }

    public TrasladoMapper getTrasladoMapper() {
        return trasladoMapper;
    }

    public FachadaViandas getFachadaViandas() {
        return fachadaViandas;
    }

    public FachadaHeladeras getFachadaHeladeras() {
        return fachadaHeladeras;
    }
}


