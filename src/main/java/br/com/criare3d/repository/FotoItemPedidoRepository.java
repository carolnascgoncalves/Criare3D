package br.com.criare3d.repository;
import br.com.criare3d.models.FotoItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface FotoItemPedidoRepository extends JpaRepository<FotoItemPedido,UUID> {}
