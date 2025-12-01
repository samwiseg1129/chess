package service.results;

import model.GameData;
import java.util.Collection;

public record GameListResult(Collection<GameData> games) {}
