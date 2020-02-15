package com.tgx.chess.king.base.util;

import com.tgx.chess.king.base.exception.ZException;
import com.tgx.chess.king.base.inf.ICode;
import com.tgx.chess.king.config.Code;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author william.d.zk
 * @date 2018/1/3
 */
public enum Units
{
    /**
     * 人民币
     */
    CURRENCY_RMB("￥%f", 1, 2, SignModel.FRONT)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case CURRENCY_EUR:
                    return data / CURRENCY_EUR.getSI();
                case CURRENCY_USD:
                    return data / CURRENCY_USD.getSI();
                case CURRENCY_RMB:
                    return data;
                default:
                    throw new IllegalArgumentException();
            }
        }

    },
    CURRENCY_EUR("€%f", 7.93f, 2, SignModel.FRONT)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case CURRENCY_EUR:
                    return data;
                case CURRENCY_USD:
                    return data * getSI() / CURRENCY_USD.getSI();
                case CURRENCY_RMB:
                    return data * getSI();
                default:
                    throw new IllegalArgumentException();
            }
        }

    },
    CURRENCY_USD("[＄|\\$]%f", 6.84f, 2, SignModel.FRONT)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case CURRENCY_EUR:
                    return data * getSI() / CURRENCY_EUR.getSI();
                case CURRENCY_USD:
                    return data;
                case CURRENCY_RMB:
                    return data * getSI();
                default:
                    throw new IllegalArgumentException();
            }
        }

    },
    /**
     * 吨
     */
    WEIGHT_TON("%dT", 1000, 2, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case WEIGHT_KG:
                    return data * getSI();
                case WEIGHT_G:
                    return data * getSI() / WEIGHT_G.getSI();
                case WEIGHT_TON:
                    return data;
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 千克
     */
    WEIGHT_KG("%dKG", 1, 1, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case WEIGHT_TON:
                    return data / WEIGHT_TON.getSI();
                case WEIGHT_G:
                    return data / WEIGHT_G.getSI();
                case WEIGHT_KG:
                    return data;
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 克
     */
    WEIGHT_G("%dG", 0.001f, 0, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case WEIGHT_TON:
                    return data * getSI() / WEIGHT_TON.getSI();
                case WEIGHT_KG:
                    return data * getSI();
                case WEIGHT_G:
                    return data;
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 升
     */
    VOLUME_L("%dL", 1, 1, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case VOLUME_ML:
                    return data / VOLUME_ML.getSI();
                case VOLUME_CubicM:
                    return data / VOLUME_CubicM.getSI();
                case VOLUME_L:
                    return data;
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 毫升
     */
    VOLUME_ML("%dML", 0.001f, 0, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case VOLUME_L:
                    return data * getSI();
                case VOLUME_CubicM:
                    return data * getSI() / VOLUME_CubicM.getSI();
                case VOLUME_ML:
                    return data;
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 立方米
     */
    VOLUME_CubicM("%dM3", 1000, 2, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case VOLUME_L:
                    return data * getSI();
                case VOLUME_ML:
                    return data * getSI() / VOLUME_ML.getSI();
                case VOLUME_CubicM:
                    return data;
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 米
     */
    LENGTH_M("%dM", 1, 2, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case LENGTH_M:
                    return data;
                case LENGTH_CM:
                    return data / LENGTH_CM.getSI();
                case LENGTH_MM:
                    return data / LENGTH_MM.getSI();
                case LENGTH_KM:
                    return data / LENGTH_KM.getSI();

                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 厘米
     */
    LENGTH_CM("%dCM", 0.01f, 0, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case LENGTH_M:
                    return data * getSI();
                case LENGTH_CM:
                    return data;
                case LENGTH_MM:
                    return data * getSI() / LENGTH_MM.getSI();
                case LENGTH_KM:
                    return data * getSI() / LENGTH_KM.getSI();
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 毫米
     */
    LENGTH_MM("%dMM", 0.001f, 0, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case LENGTH_M:
                    return data * getSI();
                case LENGTH_CM:
                    return data * getSI() / LENGTH_CM.getSI();
                case LENGTH_MM:
                    return data;
                case LENGTH_KM:
                    return data * getSI() / LENGTH_KM.getSI();
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 千米
     */
    LENGTH_KM("%dKM", 1000, 2, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case LENGTH_M:
                    return data * getSI();
                case LENGTH_CM:
                    return data * getSI() / LENGTH_CM.getSI();
                case LENGTH_MM:
                    return data * getSI() / LENGTH_MM.getSI();
                case LENGTH_KM:
                    return data;
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 小时
     */
    TIME_H("%dH", 60, 2, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case TIME_M:
                    return data * getSI();
                case TIME_S:
                    return data * getSI() / TIME_S.getSI();
                case TIME_H:
                    return data;
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 分钟
     */
    TIME_M("%dM", 1, 1, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case TIME_H:
                    return data / TIME_H.getSI();
                case TIME_S:
                    return data / TIME_S.getSI();
                case TIME_M:
                    return data;
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 秒
     */
    TIME_S("%dS", 1.0f / 60, 0, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case TIME_H:
                    return data * getSI() / TIME_H.getSI();
                case TIME_M:
                    return data * getSI();
                case TIME_S:
                    return data;
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 千米每小时
     */
    SPEED_KM_H("%fKM/H", 60000f, 3, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {

            switch (to)
            {
                case SPEED_M_S:
                    return data * getSI() / SPEED_M_S.getSI();
                case SPEED_M_M:
                    return data * getSI() / SPEED_M_M.getSI();
                case SPEED_KM_H:
                    return data;
                default:
                    throw new IllegalArgumentException();
            }

        }
    },
    /**
     * 米每秒
     */
    SPEED_M_S("%fM/S", 1f / 60, 0, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case SPEED_M_S:
                    return data;
                case SPEED_M_M:
                    return data * getSI() / SPEED_M_M.getSI();
                case SPEED_KM_H:
                    return data * getSI() / SPEED_KM_H.getSI();
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 米每分钟
     */
    SPEED_M_M("%fM/M", 1f, 0, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case SPEED_M_S:
                    return data * getSI() / SPEED_M_S.getSI();
                case SPEED_M_M:
                    return data;
                case SPEED_KM_H:
                    return data * getSI() / SPEED_KM_H.getSI();
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 每米人民币价格
     */
    PRICE_DIS_CNY_M("￥%f/M", 1f, 0, SignModel.MIDDLE)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case PRICE_DIS_USD_KM:
                    return data / PRICE_DIS_USD_KM.getSI();
                case PRICE_DIS_USD_M:
                    return data / PRICE_DIS_USD_M.getSI();
                case PRICE_DIS_CNY_KM:
                    return data / PRICE_DIS_CNY_KM.getSI();
                case PRICE_DIS_CNY_M:
                    return data * getSI();
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 每千米人民币价格
     */
    PRICE_DIS_CNY_KM("￥%f/KM", 1.0f / 1000, 3, SignModel.MIDDLE)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case PRICE_DIS_USD_KM:
                    return data * getSI() / PRICE_DIS_USD_KM.getSI();
                case PRICE_DIS_USD_M:
                    return data * getSI() / PRICE_DIS_USD_M.getSI();
                case PRICE_DIS_CNY_KM:
                    return data * getSI();
                case PRICE_DIS_CNY_M:
                    return data * getSI() / PRICE_DIS_CNY_M.getSI();
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 每千米美元价格
     */
    PRICE_DIS_USD_KM("[＄|\\$]%f/KM", 6.84f / 1000, 5, SignModel.MIDDLE)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case PRICE_DIS_CNY_KM:
                    return data * getSI() / PRICE_DIS_CNY_KM.getSI();
                case PRICE_DIS_USD_KM:
                    return data * getSI();
                case PRICE_DIS_USD_M:
                    return data * getSI() / PRICE_DIS_USD_M.getSI();
                case PRICE_DIS_CNY_M:
                    return data * getSI() / PRICE_DIS_CNY_M.getSI();
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 每米美元价格
     */
    PRICE_DIS_USD_M("[＄|\\$]%f/M", 6.84f, 5, SignModel.MIDDLE)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case PRICE_DIS_CNY_KM:
                    return data * getSI() / PRICE_DIS_CNY_KM.getSI();
                case PRICE_DIS_USD_KM:
                    return data * getSI() / PRICE_DIS_USD_KM.getSI();
                case PRICE_DIS_USD_M:
                    return data * getSI();
                case PRICE_DIS_CNY_M:
                    return data * getSI() / PRICE_DIS_CNY_M.getSI();
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    /**
     * 分/立方
     * e.g 装货速度，卸货速度
     */
    TIME_VOLUME_M_CubicM("M/M3", 1f / 1000, 3, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case TIME_VOLUME_M_CubicM:
                    return data * getSI();
                case TIME_VOLUME_M_L:
                    return data * getSI() / TIME_VOLUME_M_L.getSI();
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    TIME_VOLUME_M_L("M/L", 1f, 3, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case TIME_VOLUME_M_CubicM:
                    return data / TIME_VOLUME_M_CubicM.getSI();
                case TIME_VOLUME_M_L:
                    return data;
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    PROGRAM_STORAGE_B("B", 1f / 1024, 0, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case PROGRAM_STORAGE_B:
                    return data;
                case PROGRAM_STORAGE_KB:
                    return data * getSI();
                case PROGRAM_STORAGE_MB:
                    return data * getSI() / PROGRAM_STORAGE_MB.getSI();
                case PROGRAM_STORAGE_GB:
                    return data * getSI() / PROGRAM_STORAGE_GB.getSI();
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    PROGRAM_STORAGE_KB("KB", 1f, 4, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case PROGRAM_STORAGE_B:
                    return data * 1024;
                case PROGRAM_STORAGE_MB:
                    return data / PROGRAM_STORAGE_MB.getSI();
                case PROGRAM_STORAGE_GB:
                    return data / PROGRAM_STORAGE_GB.getSI();
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    PROGRAM_STORAGE_MB("MB", 1024f, 3, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case PROGRAM_STORAGE_B:
                    return data * 1024 * 1024;
                case PROGRAM_STORAGE_KB:
                    return data * getSI();
                case PROGRAM_STORAGE_GB:
                    return data * getSI() / PROGRAM_STORAGE_GB.getSI();
                default:
                    throw new IllegalArgumentException();
            }
        }
    },
    PROGRAM_STORAGE_GB("GB", 1024 * 1024f, 3, SignModel.BEHIND)
    {
        @Override
        public float translate(Units to, float data)
        {
            switch (to)
            {
                case PROGRAM_STORAGE_B:
                    return data * 1024 * 1024 * 1024;
                case PROGRAM_STORAGE_KB:
                    return data * getSI();
                case PROGRAM_STORAGE_MB:
                    return data * getSI() / PROGRAM_STORAGE_MB.getSI();
                default:
                    throw new IllegalArgumentException();
            }
        }
    };

    public Triple<Boolean,
                  Float,
                  Units>

            parse(String input)
    {
        Float value = _Model.getValue(_Pattern, input);
        return Objects.nonNull(value) ? new Triple<>(true, value * getSI(), this)
                                      : new Triple<>(false, 0.0f, null);
    }

    public float getValue(String input)
    {
        Float value = _Model.getValue(_Pattern, input);
        return value == null ? 0.0f
                             : value * getSI();
    }

    private final String    _SignFormatter;
    private final int       _Precision;
    private final float     _SI;
    private final SignModel _Model;
    private final Pattern   _Pattern;

    enum SignModel
    {
        /**
         * 
         */
        FRONT
        {
            @Override
            Pattern getPattern(String sign)
            {
                return Pattern.compile("\\s*("
                                       + sign.replaceAll("%[d|f]", "")
                                       + ")\\s*"
                                       + RegExUtil.DOUBLE_PATTERN_STRING);
            }

            @Override
            Float getValue(Pattern pattern, String input)
            {
                Matcher matcher = pattern.matcher(input.toUpperCase());

                return matcher.matches() ? Float.parseFloat(matcher.group(2))
                                         : null;
            }
        },
        /**
         * 
         */
        MIDDLE
        {
            @Override
            Pattern getPattern(String sign)
            {
                String[] split = sign.split("%[d|f]", 2);
                return Pattern.compile("\\s*("
                                       + split[0]
                                       + ")\\s*"
                                       + RegExUtil.DOUBLE_PATTERN_STRING
                                       + "\\s*("
                                       + split[1]
                                       + ")\\s*");
            }

            @Override
            Float getValue(Pattern pattern, String input)
            {
                Matcher matcher = pattern.matcher(input.toUpperCase());

                return matcher.matches() ? Float.parseFloat(matcher.group(2))
                                         : null;
            }
        },
        /**
         
         */
        BEHIND
        {
            @Override
            Pattern getPattern(String sign)
            {
                return Pattern.compile(RegExUtil.DOUBLE_PATTERN_STRING
                                       + "\\s*("
                                       + sign.replaceAll("%[d|f]", "")
                                       + ")\\s*");
            }

            @Override
            Float getValue(Pattern pattern, String input)
            {
                Matcher matcher = pattern.matcher(input.toUpperCase());

                return matcher.matches() ? Float.parseFloat(matcher.group(1))
                                         : null;
            }
        };

        abstract Pattern getPattern(String sign);

        abstract Float getValue(Pattern pattern, String input);
    }

    Units(String formatter,
          float si,
          int precision,
          SignModel model)
    {
        _SignFormatter = formatter;
        _Model = model;
        _SI = si;
        _Precision = precision;
        _Pattern = model.getPattern(formatter);
    }

    float getSI()
    {
        return _SI;
    }

    public String getSign()
    {
        return _SignFormatter;
    }

    public int getPrecision()
    {
        return _Precision;
    }

    public String getString(float data)
    {
        return String.format(getSign().replaceAll("%[d|f]", "%." + _Precision + "f"), data);
    }

    public abstract float translate(Units to, float data);

    final static String UNIFIC_WEIGHT          = "kilogram";
    final static String UNIFIC_VOLUME          = "litre";
    final static String UNIFIC_LENGTH          = "meter";
    final static String UNIFIC_TIME            = "minute";
    final static String UNIFIC_PROGRAM_STORAGE = "kilobyte";

    public static String getUnificLength()
    {
        return UNIFIC_LENGTH;
    }

    public static String getUnificTime()
    {
        return UNIFIC_TIME;
    }

    public static String getUnificVolume()
    {
        return UNIFIC_VOLUME;
    }

    public static String getUnificWeight()
    {
        return UNIFIC_WEIGHT;
    }

    public static String getDefaultLengthSign()
    {
        return LENGTH_M.getSign();
    }

    public static String getDefaultWeightSign()
    {
        return WEIGHT_KG.getSign();
    }

    public static String getDefaultVolumeSign()
    {
        return VOLUME_L.getSign();
    }

    public static String getDefaultTimeSign()
    {
        return TIME_M.getSign();
    }

    public static String getDefaultCoinSign()
    {
        return CURRENCY_RMB.getSign();
    }

    public static String getDefaultProgramStorageSign()
    {
        return PROGRAM_STORAGE_KB.getSign();
    }

    public static Units getDefaultLengthUnit()
    {
        return LENGTH_M;
    }

    public static Units getDefaultWeightUnit()
    {
        return WEIGHT_KG;
    }

    public static Units getDefaultVolumeUnit()
    {
        return VOLUME_L;
    }

    public static Units getDefaultTimeUnit()
    {
        return TIME_M;
    }

    public static Units getDefaultCoinUnit()
    {
        return CURRENCY_RMB;
    }

    public static Units getDefaultSpeedUnit()
    {
        return SPEED_M_M;
    }

    public static Units getDefaultProgramStorageUnit()
    {
        return PROGRAM_STORAGE_KB;
    }

    public static float parse(String input, String catalog, ICode errorCode) throws ZException
    {
        return Stream.of(Units.values())
                     .map(units -> units.parse(input))
                     .filter(Triple::first)
                     .findAny()
                     .orElseThrow(() -> new ZException(errorCode.getMsg(catalog, input)))
                     .second();
    }

    public static float parseValue(String input) throws ZException
    {
        return Stream.of(Units.values())
                     .map(units -> units.parse(input))
                     .filter(Triple::first)
                     .findAny()
                     .orElseThrow(() -> new ZException(Code.ILLEGAL_PARAM.getMsg(input)))
                     .second();
    }
}