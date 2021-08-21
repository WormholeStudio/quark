package studio.wormhole.quark.command.alma.airdrop;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
public class CSVRecord {

  @CsvBindByName(column = "address", required = true)
  private String address;
  @CsvBindByName(column = "amount", required = true)
  private BigInteger amount;
  private long index;


}
