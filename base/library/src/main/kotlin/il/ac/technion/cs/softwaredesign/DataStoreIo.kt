package il.ac.technion.cs.softwaredesign

/*
reason for this class existence:
to limit the dependency of the data-store library, we use a wrapper class for easier refactoring at the future.
*/
class DataStoreIo {
    companion object{

        fun write(key: String, data: String){
//            return il.ac.technion.cs.softwaredesign.storage.write(key, data)
           // TODO(cast to byteArray)
        }

        fun read(key: String): String?{
//            return il.ac.technion.cs.softwaredesign.storage.read(key)
            // TODO(cast to byteArray)
            return null
        }
    }
}
